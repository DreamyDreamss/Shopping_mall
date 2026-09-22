"""KT알파쇼핑 수집분(raw_*.txt) → catalog.json + images/{sku}.jpg (사내 로컬 테스트베드 한정).

사용: python build_kshop_catalog.py raw_2026-09-19.txt
- 기존 공개 라이선스 카탈로그는 catalog_openverse.json 으로 보존한다(처음 1회).
- 필드는 기존 catalog.json 계약 + kshop 속성(tv_product·installment_months·free_shipping·card_discount_pct·source_id).
"""
import io
import json
import os
import re
import shutil
import sys
import time
import urllib.request

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
CAT_DIR = os.path.dirname(HERE)
IMG_DIR = os.path.join(CAT_DIR, "images")
UA = "sl-shop-testbed/1.0 (internal local lab, user-authorized)"
LICENSE = "KT알파쇼핑 — 사내 로컬 테스트베드 한정 사용(사용자 허락, 외부 배포·공개 저장소 반입 금지)"

# 대분류 9 · 중분류(카테고리 화면 탭 실측) · 키워드 → 중분류
TREE = [
    ("70000003", "APL", "의류/언더웨어", [("W", "여성의류", "여성"), ("M", "남성의류", "남성"), ("K", "유아동패션/용품", "유아|키즈")]),
    ("70000059", "SHO", "패션슈즈/잡화", [("SH", "신발", "스니커즈|러닝화|슬리퍼|신발"), ("BG", "가방", "백|가방"), ("JW", "쥬얼리", "골드|순금|목걸이")]),
    ("70000137", "BTY", "화장품/이미용", [("SK", "스킨케어", "토닝|크림|팩|앰플"), ("MU", "메이크업", "팩트|쿠션"), ("HB", "헤어/바디케어", "샴푸|염색")]),
    ("70000178", "DGT", "가전/디지털", [("SE", "계절가전", "매트|온열"), ("KA", "주방가전", "밥솥"), ("LA", "생활가전", "정리기|청소기")]),
    ("70000249", "SPT", "스포츠/레저", [("SW", "스포츠의류", "팬츠|모크넥|점퍼|셋업")]),
    ("70000329", "FOD", "식품/건강", [("MT", "축산물", "갈비|찜갈비"), ("SF", "수산물", "새우"), ("SN", "간식/떡", "송편|견과"), ("HL", "건강식품", "홍삼"), ("RD", "간편조리", "갈비탕")]),
    ("70000406", "LIV", "주방/생활/애견", [("PS", "개인위생용품", "치약"), ("DT", "세제/섬유유연제", "세제"), ("TS", "화장지/물티슈", "화장지|키친타월|물티슈"), ("KT", "주방용품", "매직핸즈|텀블러")]),
    ("70000493", "FUR", "가구/인테리어", [("BD", "침구", "침구|이불|베개"), ("OF", "학생/서재/사무용가구", "의자|책장"), ("DC", "인테리어소품", "매트|방석")]),
    ("70000539", "TRV", "여행/문화", [("TR", "여행/항공권", "여행|패키지|숙박|호텔"), ("TK", "전시회/공연", "이용권")]),
]


def norm_img(p):
    p = p.strip().replace("imgs.kshop.co.kr/", "").lstrip("/")
    if not (p.startswith("d2/") or p.startswith("goods/")):
        p = "d2/product/emc/" + p
    return "https://imgs.kshop.co.kr/" + p


def won(s):
    return int(s.replace(",", ""))


def parse(line):
    text, _, img = line.partition(" @@ ")
    tv = text.startswith("[TV상품]")
    name = text[len("[TV상품]"):] if tv else text
    m = re.search(r"\s(\d{1,3}(?:,\d{3})+|\d+)원", name)
    name, rest = name[:m.start()].strip(), name[m.start():]
    prices = [won(x) for x in re.findall(r"(\d{1,3}(?:,\d{3})+|\d+)원", rest)]
    disc = re.search(r"할인율 (\d+)%", rest)
    price = prices[-1]
    list_price = prices[0] if disc and len(prices) >= 2 and prices[0] > price else None
    inst = re.search(r"무이자 (\d+)", rest)
    card = re.search(r"청구 할인 (\d+)%", rest)
    return {
        "name": name, "price": price, "list_price": list_price,
        "tv_product": tv, "free_shipping": "무료 배송" in rest,
        "installment_months": int(inst.group(1)) if inst else None,
        "card_discount_pct": int(card.group(1)) if card else None,
        "img_url": norm_img(img),
        "source_id": re.search(r"/([A-Z]?\d+)_(?:550|g)_", img).group(1) if re.search(r"/([A-Z]?\d+)_(?:550|g)_", img) else None,
    }


def brand_of(name):
    m = re.match(r"^[\[(]([^\])]+)[\])]", name)
    if m and not re.search(r"특가|정품|할인|핫딜|클리어런스|공식|단독|매장|\d", m.group(1)):
        return m.group(1).strip()
    for tok in re.split(r"[\s\[\]()]+", name):
        if len(tok) >= 2 and not re.search(r"\d|★|정품|핫딜|단독|특가", tok):
            return tok
    return "기타"


def square(data):
    im = Image.open(io.BytesIO(data)).convert("RGB")
    s = max(im.size)
    canvas = Image.new("RGB", (s, s), (255, 255, 255))
    canvas.paste(im, ((s - im.width) // 2, (s - im.height) // 2))
    return canvas.resize((800, 800), Image.LANCZOS)


def main(raw_path):
    ov = os.path.join(CAT_DIR, "catalog_openverse.json")
    cur = os.path.join(CAT_DIR, "catalog.json")
    if os.path.exists(cur) and not os.path.exists(ov):
        shutil.copy(cur, ov)
    ov_img = os.path.join(CAT_DIR, "images_openverse")
    if os.path.isdir(IMG_DIR) and not os.path.isdir(ov_img):
        shutil.copytree(IMG_DIR, ov_img)

    tops = {t[0]: t for t in TREE}
    categories, brands, products, seen = [], {}, [], set()
    for cid, code, name, mids in TREE:
        categories.append({"code": code, "name": name, "source_id": cid,
                           "children": [{"code": code + mc, "name": mn, "children": []} for mc, mn, _ in mids]})
    top = None
    n = 3001
    for line in open(raw_path, encoding="utf-8"):
        line = line.strip()
        if not line:
            continue
        if line.startswith("CAT "):
            top = tops[line.split()[1]]
            continue
        p = parse(line)
        if p["name"] in seen:
            continue
        seen.add(p["name"])
        _, code, _, mids = top
        leaf = next((code + mc for mc, _, kw in mids if re.search(kw, p["name"])), code + mids[0][0])
        b = brand_of(p["name"])
        # 순번 코드 — hash()는 실행마다 달라져 재적재 멱등이 깨진다
        brands.setdefault(b, f"KB{len(brands) + 1:03d}")
        sku = f"SKU-{n}"
        n += 1
        req = urllib.request.Request(p["img_url"], headers={"User-Agent": UA})
        try:
            img = square(urllib.request.urlopen(req, timeout=30).read())
            img.save(os.path.join(IMG_DIR, f"{sku}.jpg"), quality=85)
        except Exception as e:  # noqa: BLE001
            print("이미지 실패", sku, p["name"], e)
            n -= 1
            continue
        time.sleep(0.5)
        products.append({
            "sku": sku, "name": p["name"], "brand": brands[b], "category": leaf,
            "price": p["price"], "list_price": p["list_price"], "stock_qty": 50, "sale_yn": "Y",
            "description": "", "tags": [], "image": f"images/{sku}.jpg",
            "tv_product": p["tv_product"], "free_shipping": p["free_shipping"],
            "installment_months": p["installment_months"], "card_discount_pct": p["card_discount_pct"],
            "source_id": p["source_id"], "source_url": p["img_url"], "author": "KT알파쇼핑",
            "license": LICENSE, "license_url": None,
        })
        print(sku, p["name"][:40], p["price"])
    out = {"source": "KT알파쇼핑(kshop.co.kr) 2026-09-19 수집 — " + LICENSE,
           "categories": categories,
           "brands": [{"code": c, "name": nm} for nm, c in brands.items()],
           "products": products}
    json.dump(out, open(os.path.join(CAT_DIR, "catalog.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("상품", len(products), "브랜드", len(brands), "대분류", len(categories))


if __name__ == "__main__":
    main(os.path.join(HERE, sys.argv[1]))
