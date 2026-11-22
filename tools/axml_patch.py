import struct
import sys
import zipfile

NEW_PKG = "com.lyano.playstore.patch"
NEW_VER = 999000000

ATTR_STRING = 0x03


def read_pool(data, off):
    typ, hsz, size = struct.unpack_from("<HHI", data, off)
    assert typ == 1, "string pool expected at 0x%x, got 0x%x" % (off, typ)
    sc, styc, flags, str_start, sty_start = struct.unpack_from("<IIIII", data, off + 8)
    utf8 = bool(flags & 0x100)
    offs = struct.unpack_from("<%dI" % sc, data, off + 28)
    strings = []
    for so in offs:
        p = off + str_start + so
        if utf8:
            n = data[p]
            if n & 0x80:
                n = ((n & 0x7F) << 8) | data[p + 1]
                p += 2
            else:
                p += 1
            m = data[p]
            if m & 0x80:
                m = ((m & 0x7F) << 8) | data[p + 1]
                p += 2
            else:
                p += 1
            strings.append(data[p:p + m].decode("utf-8"))
        else:
            n = struct.unpack_from("<H", data, p)[0]
            if n & 0x8000:
                n = ((n & 0x7FFF) << 16) | struct.unpack_from("<H", data, p + 2)[0]
                p += 4
            else:
                p += 2
            strings.append(data[p:p + 2 * n].decode("utf-16-le"))
    stysoffs = ()
    styles = b""
    if styc:
        stysoffs = struct.unpack_from("<%dI" % styc, data, off + 28 + 4 * sc)
        styles = data[off + sty_start: off + size]
    return strings, utf8, stysoffs, styles, off + size


def enc8(v):
    if v > 0x7F:
        return bytes([0x80 | ((v >> 8) & 0x7F), v & 0xFF])
    return bytes([v])


def build_pool(strings, utf8, stysoffs, styles):
    n = len(strings)
    offsets = []
    blob = bytearray()
    for s in strings:
        offsets.append(len(blob))
        if utf8:
            u16len = len(s.encode("utf-16-le")) // 2
            sb = s.encode("utf-8")
            blob += enc8(u16len) + enc8(len(sb)) + sb + b"\x00"
        else:
            sb = s.encode("utf-16-le")
            nch = len(sb) // 2
            if nch > 0x7FFF:
                blob += struct.pack("<HH", 0x8000 | (nch >> 16), nch & 0xFFFF)
            else:
                blob += struct.pack("<H", nch)
            blob += sb + b"\x00\x00"
    while len(blob) % 4:
        blob += b"\x00"
    str_start = 28 + 4 * n + 4 * len(stysoffs)
    flags = 0x100 if utf8 else 0
    sty_start = str_start + len(blob) if stysoffs else 0
    chunk = bytearray(struct.pack("<HHI", 1, 28, 0))
    chunk += struct.pack("<IIIII", n, len(stysoffs), flags, str_start, sty_start)
    chunk += b"".join(struct.pack("<I", o) for o in offsets)
    chunk += b"".join(struct.pack("<I", o) for o in stysoffs)
    chunk += blob
    chunk += styles
    while len(chunk) % 4:
        chunk += b"\x00"
    struct.pack_into("<I", chunk, 4, len(chunk))
    return bytes(chunk)


def map_auth(a):
    if a.startswith("com.google.android.finsky."):
        return NEW_PKG + ".finsky" + a[len("com.google.android.finsky"):]
    if a.startswith("com.android.vending."):
        return NEW_PKG + a[len("com.android.vending"):]
    if a == "com.android.vending":
        return NEW_PKG
    return a


def patch_manifest(data):
    typ, hsz, size = struct.unpack_from("<HHI", data, 0)
    assert typ == 3, "AXML header expected"
    strings, utf8, stysoffs, styles, pool_end = read_pool(data, 8)
    strmap = {}
    for i, s in enumerate(strings):
        strmap.setdefault(s, i)

    def add(s):
        if s in strmap:
            return strmap[s]
        strings.append(s)
        strmap[s] = len(strings) - 1
        return len(strings) - 1

    rm_typ, rm_hsz, rm_size = struct.unpack_from("<HHI", data, pool_end)
    assert rm_typ == 0x0180, "resource map expected"
    rm = data[pool_end:pool_end + rm_size]
    rest = bytearray(data[pool_end + rm_size:])

    def set_str(a, idx):
        struct.pack_into("<I", rest, a + 8, idx)
        struct.pack_into("<H", rest, a + 12, 8)
        rest[a + 14] = 0
        rest[a + 15] = ATTR_STRING
        struct.pack_into("<I", rest, a + 16, idx)

    pos = 0
    edited = 0
    perm_map = {}
    while pos + 8 <= len(rest):
        ctype, chsz, csize = struct.unpack_from("<HHI", rest, pos)
        if csize < 8:
            break
        if ctype == 0x0102:
            ext = pos + chsz
            name_idx = struct.unpack_from("<I", rest, ext + 4)[0]
            if name_idx < len(strings) and strings[name_idx] == "permission":
                attr_start = struct.unpack_from("<H", rest, ext + 8)[0]
                attr_size = struct.unpack_from("<H", rest, ext + 10)[0]
                attr_count = struct.unpack_from("<H", rest, ext + 12)[0]
                for i in range(attr_count):
                    a = ext + attr_start + i * attr_size
                    nm_idx = struct.unpack_from("<I", rest, a + 4)[0]
                    if nm_idx < len(strings) and strings[nm_idx] == "name":
                        val = struct.unpack_from("<I", rest, a + 16)[0]
                        if val < len(strings):
                            perm_map[strings[val]] = map_auth(strings[val])
        pos += csize

    pos = 0
    while pos + 8 <= len(rest):
        ctype, chsz, csize = struct.unpack_from("<HHI", rest, pos)
        if csize < 8:
            break
        if ctype == 0x0102:  # start element
            ext = pos + chsz
            name_idx = struct.unpack_from("<I", rest, ext + 4)[0]
            elem = strings[name_idx] if name_idx < len(strings) else ""
            attr_start = struct.unpack_from("<H", rest, ext + 8)[0]
            attr_size = struct.unpack_from("<H", rest, ext + 10)[0]
            attr_count = struct.unpack_from("<H", rest, ext + 12)[0]
            for i in range(attr_count):
                a = ext + attr_start + i * attr_size
                nm_idx = struct.unpack_from("<I", rest, a + 4)[0]
                nm = strings[nm_idx] if nm_idx < len(strings) else ""
                dtype = rest[a + 15]
                val = struct.unpack_from("<I", rest, a + 16)[0]

                if elem == "manifest" and nm == "package":
                    set_str(a, add(NEW_PKG))
                    edited += 1
                elif elem == "manifest" and nm == "versionCode":
                    struct.pack_into("<I", rest, a + 16, NEW_VER)
                    edited += 1
                elif elem == "permission" and nm == "name" and dtype == ATTR_STRING and val < len(strings):
                    new = map_auth(strings[val])
                    if new != strings[val]:
                        set_str(a, add(new))
                        edited += 1
                elif elem in ("uses-permission", "uses-permission-sdk-23") and nm == "name" \
                        and dtype == ATTR_STRING and val < len(strings) and strings[val] in perm_map:
                    set_str(a, add(perm_map[strings[val]]))
                    edited += 1
                elif nm == "authorities" and dtype == ATTR_STRING and val < len(strings):
                    old = strings[val]
                    new = ";".join(map_auth(x.strip()) for x in old.split(";"))
                    if new != old:
                        set_str(a, add(new))
                        edited += 1
                elif nm in ("name", "targetActivity", "parentActivityName",
                            "backupAgent", "manageSpaceActivity") and dtype == ATTR_STRING and val < len(strings):
                    old = strings[val]
                    if old.startswith("."):
                        set_str(a, add("com.android.vending" + old))
                        edited += 1
        pos += csize

    pool = build_pool(strings, utf8, stysoffs, styles)
    out = bytearray(struct.pack("<HHI", 3, 8, 0))
    out += pool
    out += rm
    out += rest
    struct.pack_into("<I", out, 4, len(out))
    print("manifest attrs edited:", edited)
    return bytes(out)


def main():
    src, dst = sys.argv[1], sys.argv[2]
    with zipfile.ZipFile(src) as zin:
        manifest = patch_manifest(zin.read("AndroidManifest.xml"))
        with zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
            for item in zin.infolist():
                name = item.filename
                if name == "AndroidManifest.xml":
                    zout.writestr(item, manifest)
                    continue
                if name.startswith("META-INF/") and (
                        name.endswith(".SF") or name.endswith(".RSA") or name.endswith(".DSA")
                        or name.endswith(".EC") or name == "META-INF/MANIFEST.MF"):
                    continue
                zout.writestr(item, zin.read(name))
    print("wrote", dst)


if __name__ == "__main__":
    main()
