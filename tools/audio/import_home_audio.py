#!/usr/bin/env python3
"""
One-shot importer for the Pokémon HOME 4.0.3 BGM assets used by POKEDEX.

The importer downloads the exact public APK build whose SHA-256 matches the
user-supplied reference APK, extracts the four HOME BGM bundles, decrypts the
UnityFS header, rebuilds the FSB5/Vorbis streams as standard Ogg Vorbis files,
and writes them into app/src/main/res/raw.

This script is intentionally build-time tooling only. None of the HOME APK,
decryption material, or FSB parser is shipped in the Android app.
"""
from __future__ import annotations

import argparse
import ctypes
import ctypes.util
import hashlib
import json
import math
import os
import struct
import sys
import tempfile
import urllib.request
import zipfile
from datetime import datetime, timezone
from io import BytesIO
from pathlib import Path

try:
    import lz4.block
except ImportError as exc:
    raise SystemExit("Missing dependency: pip install lz4") from exc

PACKAGE = "jp.pokemon.pokemonhome"
VERSION = "4.0.3"
EXPECTED_APK_SHA256 = "6446971e74862c84454c86482c8f5dcc902e257950eda225b9f023bcf3555308"
API_BASE = "https://www.uptodown.app/eapi"
API_SECRET = "$(=a%·!45J&S"
UA = "Dalvik/2.1.0 (Linux; U; Android 14; SM-G955F Build/AP2A.240805.005)"
KEY = b"lrZ6++Ln5tLnBsJk.ae6J8BLaLbMhVn@"
IV = b"X6TU@VYU$HyqKy57PfwWg7.t7wk2oqtg"
AUDIO_ASSETS = {
    "bgm_mt_ps_01": "pokehome_ps_01.ogg",
    "bgm_mt_st_sys01": "pokehome_st_sys01.ogg",
    "bgm_mt_st_sys02": "pokehome_st_sys02.ogg",
    "bgm_mt_st_sys03": "pokehome_st_sys03.ogg",
}

SBOX = [
0x63,0x7c,0x77,0x7b,0xf2,0x6b,0x6f,0xc5,0x30,0x01,0x67,0x2b,0xfe,0xd7,0xab,0x76,
0xca,0x82,0xc9,0x7d,0xfa,0x59,0x47,0xf0,0xad,0xd4,0xa2,0xaf,0x9c,0xa4,0x72,0xc0,
0xb7,0xfd,0x93,0x26,0x36,0x3f,0xf7,0xcc,0x34,0xa5,0xe5,0xf1,0x71,0xd8,0x31,0x15,
0x04,0xc7,0x23,0xc3,0x18,0x96,0x05,0x9a,0x07,0x12,0x80,0xe2,0xeb,0x27,0xb2,0x75,
0x09,0x83,0x2c,0x1a,0x1b,0x6e,0x5a,0xa0,0x52,0x3b,0xd6,0xb3,0x29,0xe3,0x2f,0x84,
0x53,0xd1,0x00,0xed,0x20,0xfc,0xb1,0x5b,0x6a,0xcb,0xbe,0x39,0x4a,0x4c,0x58,0xcf,
0xd0,0xef,0xaa,0xfb,0x43,0x4d,0x33,0x85,0x45,0xf9,0x02,0x7f,0x50,0x3c,0x9f,0xa8,
0x51,0xa3,0x40,0x8f,0x92,0x9d,0x38,0xf5,0xbc,0xb6,0xda,0x21,0x10,0xff,0xf3,0xd2,
0xcd,0x0c,0x13,0xec,0x5f,0x97,0x44,0x17,0xc4,0xa7,0x7e,0x3d,0x64,0x5d,0x19,0x73,
0x60,0x81,0x4f,0xdc,0x22,0x2a,0x90,0x88,0x46,0xee,0xb8,0x14,0xde,0x5e,0x0b,0xdb,
0xe0,0x32,0x3a,0x0a,0x49,0x06,0x24,0x5c,0xc2,0xd3,0xac,0x62,0x91,0x95,0xe4,0x79,
0xe7,0xc8,0x37,0x6d,0x8d,0xd5,0x4e,0xa9,0x6c,0x56,0xf4,0xea,0x65,0x7a,0xae,0x08,
0xba,0x78,0x25,0x2e,0x1c,0xa6,0xb4,0xc6,0xe8,0xdd,0x74,0x1f,0x4b,0xbd,0x8b,0x8a,
0x70,0x3e,0xb5,0x66,0x48,0x03,0xf6,0x0e,0x61,0x35,0x57,0xb9,0x86,0xc1,0x1d,0x9e,
0xe1,0xf8,0x98,0x11,0x69,0xd9,0x8e,0x94,0x9b,0x1e,0x87,0xe9,0xce,0x55,0x28,0xdf,
0x8c,0xa1,0x89,0x0d,0xbf,0xe6,0x42,0x68,0x41,0x99,0x2d,0x0f,0xb0,0x54,0xbb,0x16]
INV = [0] * 256
for _i, _v in enumerate(SBOX):
    INV[_v] = _i

def xtime(a: int) -> int:
    return (((a << 1) ^ 0x11B) & 0xFF) if a & 0x80 else ((a << 1) & 0xFF)

def gm(a: int, b: int) -> int:
    r = 0
    while b:
        if b & 1:
            r ^= a
        a = xtime(a)
        b >>= 1
    return r

def subword(w: int) -> int:
    return (SBOX[(w >> 24) & 255] << 24) | (SBOX[(w >> 16) & 255] << 16) | (SBOX[(w >> 8) & 255] << 8) | SBOX[w & 255]

def rotword(w: int) -> int:
    return ((w << 8) & 0xFFFFFFFF) | (w >> 24)

def expand_key(key: bytes, nb: int = 8):
    nk = len(key) // 4
    nr = max(nb, nk) + 6
    words = [int.from_bytes(key[i * 4:i * 4 + 4], "big") for i in range(nk)]
    rcon = 1
    for i in range(nk, nb * (nr + 1)):
        t = words[i - 1]
        if i % nk == 0:
            t = subword(rotword(t)) ^ (rcon << 24)
            rcon = xtime(rcon)
        elif nk > 6 and i % nk == 4:
            t = subword(t)
        words.append(words[i - nk] ^ t)
    return words, nr

def add_round_key(state, words, round_index: int, nb: int = 8):
    for c in range(nb):
        k = words[round_index * nb + c]
        state[0][c] ^= (k >> 24) & 255
        state[1][c] ^= (k >> 16) & 255
        state[2][c] ^= (k >> 8) & 255
        state[3][c] ^= k & 255

def inverse_shift_rows(state, nb: int = 8):
    shifts = [0, 1, 3, 4] if nb == 8 else [0, 1, 2, 3]
    for r in range(1, 4):
        s = shifts[r] % nb
        state[r] = state[r][-s:] + state[r][:-s]

def inverse_sub_bytes(state):
    for r in range(4):
        for c in range(len(state[r])):
            state[r][c] = INV[state[r][c]]

def inverse_mix_columns(state):
    nb = len(state[0])
    for c in range(nb):
        a = [state[r][c] for r in range(4)]
        state[0][c] = gm(a[0], 14) ^ gm(a[1], 11) ^ gm(a[2], 13) ^ gm(a[3], 9)
        state[1][c] = gm(a[0], 9) ^ gm(a[1], 14) ^ gm(a[2], 11) ^ gm(a[3], 13)
        state[2][c] = gm(a[0], 13) ^ gm(a[1], 9) ^ gm(a[2], 14) ^ gm(a[3], 11)
        state[3][c] = gm(a[0], 11) ^ gm(a[1], 13) ^ gm(a[2], 9) ^ gm(a[3], 14)

def decrypt_block(block: bytes, key: bytes) -> bytes:
    nb = len(block) // 4
    words, nr = expand_key(key, nb)
    state = [[0] * nb for _ in range(4)]
    for c in range(nb):
        for r in range(4):
            state[r][c] = block[4 * c + r]
    add_round_key(state, words, nr, nb)
    for rnd in range(nr - 1, 0, -1):
        inverse_shift_rows(state, nb)
        inverse_sub_bytes(state)
        add_round_key(state, words, rnd, nb)
        inverse_mix_columns(state)
    inverse_shift_rows(state, nb)
    inverse_sub_bytes(state)
    add_round_key(state, words, 0, nb)
    return bytes(state[r][c] for c in range(nb) for r in range(4))

def cbc_decrypt(data: bytes, key: bytes, iv: bytes, block_size: int = 32) -> bytes:
    if len(data) % block_size:
        raise ValueError("Encrypted header is not block aligned")
    out = []
    prev = iv
    for i in range(0, len(data), block_size):
        block = data[i:i + block_size]
        dec = decrypt_block(block, key)
        out.append(bytes(x ^ y for x, y in zip(dec, prev)))
        prev = block
    return b"".join(out)

def decrypt_aba(data: bytes) -> bytes:
    # The original HOME CryptoStream returns 1024 plaintext bytes while
    # consuming 1056 encrypted bytes because of the final PKCS#7 block.
    head = cbc_decrypt(data[:1056], KEY, IV, 32)
    pad = head[-1]
    if 1 <= pad <= 32 and head.endswith(bytes([pad]) * pad):
        head = head[:-pad]
    head = head[:1024]
    result = head + data[1056:]
    if not result.startswith(b"UnityFS"):
        raise ValueError("ABA decrypt did not produce UnityFS")
    return result

def c_string(data: bytes, pos: int):
    end = data.index(0, pos)
    return data[pos:end].decode("utf-8", "replace"), end + 1

def unity_decompress(data: bytes, kind: int, expected: int) -> bytes:
    kind &= 0x3F
    if kind == 0:
        return data
    if kind in (2, 3):
        return lz4.block.decompress(data, uncompressed_size=expected)
    raise ValueError(f"Unsupported UnityFS compression {kind}")

def extract_fsb_from_unityfs(bundle: bytes) -> bytes:
    pos = 0
    signature, pos = c_string(bundle, pos)
    if signature != "UnityFS":
        raise ValueError(signature)
    _format = struct.unpack_from(">I", bundle, pos)[0]
    pos += 4
    _player, pos = c_string(bundle, pos)
    _engine, pos = c_string(bundle, pos)
    _size, compressed_info_size, uncompressed_info_size, flags = struct.unpack_from(">QIII", bundle, pos)
    pos += 20
    if flags & 0x200:
        pos = (pos + 15) & ~15
    if flags & 0x80:
        info_offset = len(bundle) - compressed_info_size
        compressed_info = bundle[info_offset:info_offset + compressed_info_size]
        data_offset = pos
    else:
        compressed_info = bundle[pos:pos + compressed_info_size]
        pos += compressed_info_size
        if flags & 0x200:
            pos = (pos + 15) & ~15
        data_offset = pos
    info = unity_decompress(compressed_info, flags, uncompressed_info_size)
    cursor = 16
    block_count = struct.unpack_from(">I", info, cursor)[0]
    cursor += 4
    blocks = []
    for _ in range(block_count):
        uncompressed, compressed, block_flags = struct.unpack_from(">IIH", info, cursor)
        cursor += 10
        blocks.append((uncompressed, compressed, block_flags))
    entry_count = struct.unpack_from(">I", info, cursor)[0]
    cursor += 4
    entries = []
    for _ in range(entry_count):
        offset, size, entry_flags = struct.unpack_from(">QQI", info, cursor)
        cursor += 20
        name, cursor = c_string(info, cursor)
        entries.append((offset, size, entry_flags, name))
    stream = bytearray()
    read_pos = data_offset
    for uncompressed, compressed, block_flags in blocks:
        chunk = bundle[read_pos:read_pos + compressed]
        read_pos += compressed
        stream.extend(unity_decompress(chunk, block_flags, uncompressed))
    for offset, size, _entry_flags, name in entries:
        payload = bytes(stream[offset:offset + size])
        if name.endswith(".resource") and payload.startswith(b"FSB5"):
            return payload
    raise ValueError("FSB5 .resource not found in UnityFS bundle")

# Minimal Ogg/Vorbis rebuild -------------------------------------------------
class VorbisInfo(ctypes.Structure):
    _fields_ = [
        ("version", ctypes.c_int), ("channels", ctypes.c_int), ("rate", ctypes.c_long),
        ("bitrate_upper", ctypes.c_long), ("bitrate_nominal", ctypes.c_long),
        ("bitrate_lower", ctypes.c_long), ("bitrate_window", ctypes.c_long),
        ("codec_setup", ctypes.c_void_p),
    ]
class VorbisComment(ctypes.Structure):
    _fields_ = [
        ("user_comments", ctypes.POINTER(ctypes.c_char_p)),
        ("comment_lengths", ctypes.POINTER(ctypes.c_int)),
        ("comments", ctypes.c_int), ("vendor", ctypes.c_char_p),
    ]
class OggStreamState(ctypes.Structure):
    _fields_ = [
        ("body_data", ctypes.POINTER(ctypes.c_ubyte)), ("body_storage", ctypes.c_long),
        ("body_fill", ctypes.c_long), ("body_returned", ctypes.c_long),
        ("lacing_vals", ctypes.POINTER(ctypes.c_int)),
        ("granule_vals", ctypes.POINTER(ctypes.c_longlong)),
        ("lacing_storage", ctypes.c_long), ("lacing_fill", ctypes.c_long),
        ("lacing_packet", ctypes.c_long), ("lacing_returned", ctypes.c_long),
        ("header", ctypes.c_ubyte * 282), ("header_fill", ctypes.c_int),
        ("e_o_s", ctypes.c_int), ("b_o_s", ctypes.c_int),
        ("serialno", ctypes.c_long), ("pageno", ctypes.c_long),
        ("packetno", ctypes.c_longlong), ("granulepos", ctypes.c_longlong),
    ]
class OggPacket(ctypes.Structure):
    _fields_ = [
        ("packet", ctypes.POINTER(ctypes.c_ubyte)), ("bytes", ctypes.c_long),
        ("b_o_s", ctypes.c_long), ("e_o_s", ctypes.c_long),
        ("granulepos", ctypes.c_longlong), ("packetno", ctypes.c_longlong),
    ]
class OggPage(ctypes.Structure):
    _fields_ = [
        ("header", ctypes.POINTER(ctypes.c_ubyte)), ("header_len", ctypes.c_long),
        ("body", ctypes.POINTER(ctypes.c_ubyte)), ("body_len", ctypes.c_long),
    ]

def load_audio_libs():
    vorbis_name = ctypes.util.find_library("vorbis")
    ogg_name = ctypes.util.find_library("ogg")
    if not vorbis_name or not ogg_name:
        raise RuntimeError("libvorbis/libogg not available")
    vorbis = ctypes.CDLL(vorbis_name)
    ogg = ctypes.CDLL(ogg_name)
    vorbis.vorbis_info_init.argtypes = [ctypes.POINTER(VorbisInfo)]
    vorbis.vorbis_info_clear.argtypes = [ctypes.POINTER(VorbisInfo)]
    vorbis.vorbis_comment_init.argtypes = [ctypes.POINTER(VorbisComment)]
    vorbis.vorbis_comment_clear.argtypes = [ctypes.POINTER(VorbisComment)]
    vorbis.vorbis_synthesis_headerin.argtypes = [ctypes.POINTER(VorbisInfo), ctypes.POINTER(VorbisComment), ctypes.POINTER(OggPacket)]
    vorbis.vorbis_synthesis_headerin.restype = ctypes.c_int
    vorbis.vorbis_packet_blocksize.argtypes = [ctypes.POINTER(VorbisInfo), ctypes.POINTER(OggPacket)]
    vorbis.vorbis_packet_blocksize.restype = ctypes.c_long
    ogg.ogg_stream_init.argtypes = [ctypes.POINTER(OggStreamState), ctypes.c_int]
    ogg.ogg_stream_init.restype = ctypes.c_int
    ogg.ogg_stream_clear.argtypes = [ctypes.POINTER(OggStreamState)]
    ogg.ogg_stream_packetin.argtypes = [ctypes.POINTER(OggStreamState), ctypes.POINTER(OggPacket)]
    ogg.ogg_stream_pageout.argtypes = [ctypes.POINTER(OggStreamState), ctypes.POINTER(OggPage)]
    ogg.ogg_stream_pageout.restype = ctypes.c_int
    ogg.ogg_stream_flush.argtypes = [ctypes.POINTER(OggStreamState), ctypes.POINTER(OggPage)]
    ogg.ogg_stream_flush.restype = ctypes.c_int
    return vorbis, ogg

def packet(data: bytes, *, bos=0, eos=0, granule=0, packet_no=0):
    arr = (ctypes.c_ubyte * len(data)).from_buffer_copy(data)
    p = OggPacket(ctypes.cast(arr, ctypes.POINTER(ctypes.c_ubyte)), len(data), bos, eos, granule, packet_no)
    p._buffer_ref = arr
    return p

def identification_header(channels: int, rate: int):
    data = (
        b"\x01vorbis" + struct.pack("<I", 0) + bytes([channels]) + struct.pack("<I", rate) +
        struct.pack("<iii", 0, 0, 0) + bytes([(8 & 0xF) | ((11 & 0xF) << 4)]) + b"\x01"
    )
    return packet(data, bos=1, packet_no=0)

def comment_header():
    vendor = b"POKEDEX HOME audio import"
    data = b"\x03vorbis" + struct.pack("<I", len(vendor)) + vendor + struct.pack("<I", 0) + b"\x01"
    return packet(data, packet_no=1)

def write_pages(ogg, state, out: BytesIO, flush=False):
    page = OggPage()
    fn = ogg.ogg_stream_flush if flush else ogg.ogg_stream_pageout
    while fn(ctypes.byref(state), ctypes.byref(page)):
        out.write(ctypes.string_at(page.header, page.header_len))
        out.write(ctypes.string_at(page.body, page.body_len))

def parse_fsb5(data: bytes):
    if data[:4] != b"FSB5":
        raise ValueError("Not FSB5")
    version, sample_count, sample_header_size, name_table_size, data_size, mode = struct.unpack_from("<6I", data, 4)
    if version != 1 or sample_count != 1 or mode != 15:
        raise ValueError(f"Unexpected FSB5 format: version={version}, samples={sample_count}, mode={mode}")
    pos = 0x3C
    raw = struct.unpack_from("<Q", data, pos)[0]
    pos += 8
    next_chunk = raw & 1
    freq_code = (raw >> 1) & 0xF
    channels = ((raw >> 5) & 1) + 1
    samples = (raw >> 34) & ((1 << 30) - 1)
    frequency = {1:8000, 2:11000, 3:11025, 4:16000, 5:22050, 6:24000, 7:32000, 8:44100, 9:48000}[freq_code]
    setup_crc = None
    while next_chunk:
        header = struct.unpack_from("<I", data, pos)[0]
        pos += 4
        next_chunk = header & 1
        chunk_size = (header >> 1) & 0xFFFFFF
        chunk_type = (header >> 25) & 0x7F
        chunk = data[pos:pos + chunk_size]
        pos += chunk_size
        if chunk_type == 2:
            frequency = struct.unpack_from("<I", chunk)[0]
        elif chunk_type == 1:
            channels = chunk[0] + 1
        elif chunk_type == 11:
            setup_crc = struct.unpack_from("<I", chunk)[0]
    data_start = 0x3C + sample_header_size + name_table_size
    return channels, frequency, samples, setup_crc, data[data_start:data_start + data_size]

def rebuild_ogg(fsb: bytes, setup_packet: bytes) -> bytes:
    channels, rate, total_samples, setup_crc, compressed = parse_fsb5(fsb)
    if setup_crc != 0x38AA59CE:
        raise ValueError(f"Unexpected FSB Vorbis setup: {setup_crc!r}")
    vorbis, ogg = load_audio_libs()
    info = VorbisInfo()
    comments = VorbisComment()
    vorbis.vorbis_info_init(ctypes.byref(info))
    vorbis.vorbis_comment_init(ctypes.byref(comments))
    state = OggStreamState()
    if ogg.ogg_stream_init(ctypes.byref(state), 1) != 0:
        raise RuntimeError("ogg_stream_init failed")
    out = BytesIO()
    try:
        headers = [
            identification_header(channels, rate),
            comment_header(),
            packet(setup_packet, packet_no=2),
        ]
        for header in headers:
            rc = vorbis.vorbis_synthesis_headerin(ctypes.byref(info), ctypes.byref(comments), ctypes.byref(header))
            if rc != 0:
                raise RuntimeError(f"vorbis header rejected: {rc}")
            ogg.ogg_stream_packetin(ctypes.byref(state), ctypes.byref(header))
            write_pages(ogg, state, out)
        write_pages(ogg, state, out, flush=True)

        pos = 0
        packet_no = 2
        granule = 0
        previous_block = 0
        while pos + 2 <= len(compressed):
            size = struct.unpack_from("<H", compressed, pos)[0]
            pos += 2
            if size == 0:
                break
            payload = compressed[pos:pos + size]
            pos += size
            if len(payload) != size:
                raise ValueError("Truncated FSB packet")
            packet_no += 1
            eos = 1 if pos >= len(compressed) else 0
            p = packet(payload, eos=eos, packet_no=packet_no)
            block = vorbis.vorbis_packet_blocksize(ctypes.byref(info), ctypes.byref(p))
            if block <= 0:
                raise RuntimeError(f"Invalid Vorbis block size {block}")
            if previous_block:
                granule += (block + previous_block) // 4
            p.granulepos = total_samples if eos else granule
            previous_block = block
            ogg.ogg_stream_packetin(ctypes.byref(state), ctypes.byref(p))
            write_pages(ogg, state, out)
        write_pages(ogg, state, out, flush=True)
        return out.getvalue()
    finally:
        ogg.ogg_stream_clear(ctypes.byref(state))
        vorbis.vorbis_comment_clear(ctypes.byref(comments))
        vorbis.vorbis_info_clear(ctypes.byref(info))

def api_key() -> str:
    now = datetime.now(timezone.utc)
    epoch_ms = int(now.timestamp() * 1000)
    offset_ms = now.minute * 60000 + now.second * 1000 + now.microsecond // 1000
    hour_epoch = (epoch_ms - offset_ms) // 1000
    return hashlib.sha256((API_SECRET + str(hour_epoch)).encode("utf-8")).hexdigest()

def request_json(url: str):
    req = urllib.request.Request(url, headers={
        "User-Agent": UA,
        "Identificador": "Uptodown_Android",
        "Identificador-Version": "707",
        "APIKEY": api_key(),
    })
    with urllib.request.urlopen(req, timeout=60) as response:
        return json.load(response)

def download_apk(destination: Path):
    resolved = request_json(f"{API_BASE}/apps/byPackagename/{PACKAGE}")
    inner = resolved.get("data", resolved)
    app_id = str(inner.get("appID") or inner.get("id"))
    versions = request_json(f"{API_BASE}/v3/app/{app_id}/device/1/compatible/versions?page[limit]=30&page[offset]=0").get("data", [])
    target = next((v for v in versions if str(v.get("version")) == VERSION), None)
    if not target:
        raise RuntimeError(f"Uptodown did not return HOME {VERSION}")
    file_id = str(target.get("fileID") or target.get("fileid"))
    url_data = request_json(f"{API_BASE}/apps/{app_id}/file/{file_id}/downloadUrl?update=0")
    download_url = url_data["data"]["downloadURL"]
    req = urllib.request.Request(download_url, headers={"User-Agent": UA})
    hasher = hashlib.sha256()
    with urllib.request.urlopen(req, timeout=180) as response, destination.open("wb") as out:
        while True:
            chunk = response.read(1024 * 1024)
            if not chunk:
                break
            out.write(chunk)
            hasher.update(chunk)
    digest = hasher.hexdigest()
    if digest != EXPECTED_APK_SHA256:
        raise RuntimeError(f"HOME APK SHA-256 mismatch: {digest}")
    print(f"Verified HOME {VERSION}: {digest}")

def import_audio(apk: Path, output_dir: Path, setup_packet: bytes):
    output_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(apk) as archive:
        for source_name, output_name in AUDIO_ASSETS.items():
            path = f"assets/AB/{source_name}.aba"
            encrypted = archive.read(path)
            unityfs = decrypt_aba(encrypted)
            fsb = extract_fsb_from_unityfs(unityfs)
            ogg = rebuild_ogg(fsb, setup_packet)
            target = output_dir / output_name
            target.write_bytes(ogg)
            channels, rate, samples, _crc, _data = parse_fsb5(fsb)
            print(f"{source_name}: {len(ogg)} bytes, {channels}ch {rate}Hz, {samples / rate:.3f}s -> {target}")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=Path, help="Optional local HOME 4.0.3 APK; otherwise Uptodown is used")
    parser.add_argument("--output", type=Path, default=Path("app/src/main/res/raw"))
    args = parser.parse_args()
    setup_path = Path(__file__).with_name("vcb_38aa59ce.b64")
    import base64
    setup_packet = base64.b64decode(setup_path.read_text(encoding="ascii").strip(), validate=True)
    if len(setup_packet) != 0x0EF8:
        raise RuntimeError(f"Invalid setup packet length: {len(setup_packet)}")
    if args.apk:
        apk = args.apk
        digest = hashlib.sha256(apk.read_bytes()).hexdigest()
        if digest != EXPECTED_APK_SHA256:
            raise RuntimeError(f"Local HOME APK SHA-256 mismatch: {digest}")
        import_audio(apk, args.output, setup_packet)
        return
    with tempfile.TemporaryDirectory(prefix="pokehome-audio-") as temp:
        apk = Path(temp) / "pokemon-home-4.0.3.apk"
        download_apk(apk)
        import_audio(apk, args.output, setup_packet)

if __name__ == "__main__":
    main()
