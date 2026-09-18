import json
import re
import sys
import uuid
import requests

# ==============================================================================
# HẰNG SỐ CẤU HÌNH GRAPHQL CỦA META ACCOUNTS CENTER
# ==============================================================================
GRAPHQL_URL = "https://accountscenter.facebook.com/api/graphql/"
DOC_ID_UPDATE_NAME = "9538143859625836"        # useFXIMUpdateNameMutation (Đổi tên)
DOC_ID_SEND_CODE = "27297512486584094"          # useTwoStepVerificationSendCodeMutation (Gửi OTP)
DOC_ID_VALIDATE_CODE = "26264014419868193"      # useTwoFactorLoginValidateCodeMutation (Xác thực OTP)

# ==============================================================================
# CÁC HÀM TIỆN ÍCH
# ==============================================================================
def parse_cookie_str(cookie_str: str) -> dict:
    cookies = {}
    for item in cookie_str.strip().split(";"):
        if "=" in item:
            k, v = item.strip().split("=", 1)
            cookies[k.strip()] = v.strip()
    return cookies

def extract_user_id(cookies: dict) -> str:
    return cookies.get("c_user", "")

def calculate_jazoest(dtsg: str) -> str:
    ans = sum(ord(c) for c in dtsg)
    return f"2{ans}"

def get_base_headers(user_id: str, lsd: str, friendly_name: str) -> dict:
    return {
        "accept": "*/*",
        "accept-language": "en-US,en;q=0.9",
        "content-type": "application/x-www-form-urlencoded",
        "origin": "https://accountscenter.facebook.com",
        "referer": f"https://accountscenter.facebook.com/profiles/{user_id}/name/?entrypoint=fb_account_center",
        "sec-ch-prefers-color-scheme": "light",
        "sec-ch-ua": '"Google Chrome";v="133", "Chromium";v="133", "Not_A Brand";v="24"',
        "sec-ch-ua-mobile": "?0",
        "sec-ch-ua-platform": '"Windows"',
        "sec-fetch-dest": "empty",
        "sec-fetch-mode": "cors",
        "sec-fetch-site": "same-origin",
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
        "x-asbd-id": "359341",
        "x-fb-friendly-name": friendly_name,
        "x-fb-lsd": lsd,
    }

def build_graphql_form_data(user_id: str, doc_id: str, friendly_name: str,
                            fb_dtsg: str, jazoest: str, lsd: str, variables: dict) -> dict:
    return {
        "av": user_id,
        "__user": user_id,
        "__a": "1",
        "__req": "1b",
        "__hs": "20714.HYP:accounts_center_pkg.2.1...0",
        "dpr": "1",
        "__ccg": "EXCELLENT",
        "__rev": "1047863799",
        "__comet_req": "5",
        "fb_dtsg": fb_dtsg,
        "jazoest": jazoest,
        "lsd": lsd,
        "fb_api_caller_class": "RelayModern",
        "fb_api_req_friendly_name": friendly_name,
        "server_timestamps": "true",
        "doc_id": doc_id,
        "variables": json.dumps(variables)
    }

# ==============================================================================
# BƯỚC 1: LẤY TOKEN (DTSG, LSD, JAZOEST) TỪ ACCOUNTS CENTER
# ==============================================================================
def resolve_tokens(session: requests.Session, user_id: str) -> tuple:
    print(f"\n[*] Đang truy cập Accounts Center để lấy fb_dtsg & lsd cho UID: {user_id}...")
    url = f"https://accountscenter.facebook.com/profiles/{user_id}/name/?entrypoint=fb_account_center"
    
    headers = {
        "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "sec-fetch-site": "none",
        "sec-fetch-mode": "navigate",
        "sec-fetch-dest": "document",
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
    }
    
    res = session.get(url, headers=headers)
    html = res.text

    # Bóc tách fb_dtsg
    dtsg_match = re.search(r'"DTSGInitialData",\[\],\{"token":"([^"]+)"', html)
    if not dtsg_match:
        dtsg_match = re.search(r'"dtsg":\{"token":"([^"]+)"', html)
    if not dtsg_match:
        dtsg_match = re.search(r'name="fb_dtsg" value="([^"]+)"', html)
    fb_dtsg = dtsg_match.group(1) if dtsg_match else None

    # Bóc tách lsd
    lsd_match = re.search(r'\["LSD",\[\],\{"token":"([^"]+)"', html)
    if not lsd_match:
        lsd_match = re.search(r'"token":"([^"]+)"\},323\]', html)
    if not lsd_match:
        lsd_match = re.search(r'name="lsd" value="([^"]+)"', html)
    lsd = lsd_match.group(1) if lsd_match else None

    # Tìm encryptedContext nếu có sẵn trong HTML (khi nick bị checkpoint)
    ctx_match = re.search(r'encryptedContext\\":\\"([^\\"]+)\\"', html)
    if not ctx_match:
        ctx_match = re.search(r'"encryptedContext":"([^"]+)"', html)
    encrypted_context = ctx_match.group(1) if ctx_match else None

    # Tìm email ẩn nếu có
    email_match = re.search(r'\\\"maskedContactPoint\\\":\\\"([^\\"]+)\\\"', html)
    if not email_match:
        email_match = re.search(r'"maskedContactPoint":"([^"]+)"', html)
    masked_email = email_match.group(1) if email_match else None

    if not fb_dtsg or not lsd:
        print("[!] Không tìm thấy đủ token trong HTML của Accounts Center, thử sang www.facebook.com...")
        fb_res = session.get("https://www.facebook.com/", headers=headers)
        fb_html = fb_res.text
        if not fb_dtsg:
            d_m = re.search(r'"DTSGInitialData",\[\],\{"token":"([^"]+)"', fb_html) or re.search(r'"dtsg":\{"token":"([^"]+)"', fb_html)
            fb_dtsg = d_m.group(1) if d_m else None
        if not lsd:
            l_m = re.search(r'\["LSD",\[\],\{"token":"([^"]+)"', fb_html) or re.search(r'"token":"([^"]+)"\},323\]', fb_html)
            lsd = l_m.group(1) if l_m else None

    jazoest = calculate_jazoest(fb_dtsg) if fb_dtsg else "25672"

    print(f"    [+] fb_dtsg : {fb_dtsg[:20]}..." if fb_dtsg else "    [-] fb_dtsg : None")
    print(f"    [+] lsd     : {lsd}")
    print(f"    [+] jazoest : {jazoest}")
    if encrypted_context:
        print(f"    [+] encryptedContext phát hiện sẵn trong HTML!")
    if masked_email:
        print(f"    [+] maskedContactPoint : {masked_email}")

    return fb_dtsg, lsd, jazoest, encrypted_context, masked_email

# ==============================================================================
# BƯỚC 2: GỬI LỆNH ĐỔI TÊN (useFXIMUpdateNameMutation)
# ==============================================================================
def update_name(session: requests.Session, user_id: str, fb_dtsg: str,
                jazoest: str, lsd: str, first_name: str, middle_name: str,
                last_name: str, full_name: str) -> dict:
    friendly_name = "useFXIMUpdateNameMutation"
    variables = {
        "client_mutation_id": str(uuid.uuid4()),
        "family_device_id": "device_id_fetch_datr",
        "identity_ids": [user_id],
        "full_name": full_name,
        "first_name": first_name,
        "middle_name": middle_name,
        "last_name": last_name,
        "interface": "FB_WEB"
    }

    headers = get_base_headers(user_id, lsd, friendly_name)
    data = build_graphql_form_data(user_id, DOC_ID_UPDATE_NAME, friendly_name,
                                  fb_dtsg, jazoest, lsd, variables)

    print(f"\n[*] Đang gửi GraphQL Mutation: {friendly_name} (doc_id: {DOC_ID_UPDATE_NAME})...")
    print(f"    - Tên mới: {full_name} (Họ: {last_name}, Đệm: {middle_name}, Tên: {first_name})")

    res = session.post(GRAPHQL_URL, headers=headers, data=data)
    try:
        return res.json()
    except Exception:
        return {"raw_text": res.text}

# ==============================================================================
# BƯỚC 3: GỬI MÃ XÁC THỰC EMAIL (useTwoStepVerificationSendCodeMutation)
# ==============================================================================
def send_verification_code(session: requests.Session, user_id: str, fb_dtsg: str,
                           jazoest: str, lsd: str, encrypted_context: str,
                           masked_contact_point: str) -> dict:
    friendly_name = "useTwoStepVerificationSendCodeMutation"
    variables = {
        "challenge": "EMAIL",
        "maskedContactPoint": masked_contact_point,
        "encryptedContext": encrypted_context
    }

    headers = get_base_headers(user_id, lsd, friendly_name)
    data = build_graphql_form_data(user_id, DOC_ID_SEND_CODE, friendly_name,
                                  fb_dtsg, jazoest, lsd, variables)

    print(f"\n[*] Đang gửi yêu cầu bắn mã OTP về: {masked_contact_point}...")
    res = session.post(GRAPHQL_URL, headers=headers, data=data)
    try:
        return res.json()
    except Exception:
        return {"raw_text": res.text}

# ==============================================================================
# BƯỚC 4: XÁC MINH MÃ OTP (useTwoFactorLoginValidateCodeMutation)
# ==============================================================================
def validate_otp_code(session: requests.Session, user_id: str, fb_dtsg: str,
                      jazoest: str, lsd: str, encrypted_context: str,
                      masked_contact_point: str, code: str) -> dict:
    friendly_name = "useTwoFactorLoginValidateCodeMutation"
    variables = {
        "code": {
            "sensitive_string_value": code
        },
        "method": "EMAIL",
        "flow": "SECURED_ACTION",
        "encryptedContext": encrypted_context,
        "maskedContactPoint": masked_contact_point,
        "next_uri": None,
        "trust_this_device": None
    }

    headers = get_base_headers(user_id, lsd, friendly_name)
    data = build_graphql_form_data(user_id, DOC_ID_VALIDATE_CODE, friendly_name,
                                  fb_dtsg, jazoest, lsd, variables)

    print(f"\n[*] Đang gửi mã OTP '{code}' để xác thực quyền SECURED_ACTION...")
    res = session.post(GRAPHQL_URL, headers=headers, data=data)
    try:
        return res.json()
    except Exception:
        return {"raw_text": res.text}

# ==============================================================================
# CHƯƠNG TRÌNH CHÍNH (MAIN WORKFLOW)
# ==============================================================================
def main():
    print("=" * 70)
    print(" DEMO TỰ ĐỘNG ĐỔI TÊN FACEBOOK QUA META ACCOUNTS CENTER (GRAPHQL)")
    print("=" * 70)

    # 1. Nhập Cookie
    cookie_str = input("\n[?] Nhập Cookie Facebook: ").strip()
    if not cookie_str:
        print("[!] Cookie không được để trống!")
        sys.exit(1)

    cookies = parse_cookie_str(cookie_str)
    user_id = extract_user_id(cookies)
    if not user_id:
        print("[!] Không tìm thấy c_user trong Cookie!")
        sys.exit(1)
    print(f"[+] Nhận diện UID: {user_id}")

    # 2. Nhập thông tin tên mới
    print("\n--- NHẬP THÔNG TIN TÊN MỚI ---")
    last_name = input("[?] Họ (Last Name): ").strip()
    middle_name = input("[?] Tên đệm (Middle Name - có thể để trống): ").strip()
    first_name = input("[?] Tên chính (First Name): ").strip()

    if not first_name or not last_name:
        print("[!] Họ và Tên không được để trống!")
        sys.exit(1)

    full_name_default = f"{last_name} {middle_name} {first_name}".replace("  ", " ").strip()
    full_name_input = input(f"[?] Tên hiển thị đầy đủ (Enter để dùng '{full_name_default}'): ").strip()
    full_name = full_name_input if full_name_input else full_name_default

    # 3. Khởi tạo Session
    session = requests.Session()
    session.cookies.update(cookies)

    # 4. Lấy tokens
    fb_dtsg, lsd, jazoest, encrypted_context, masked_email = resolve_tokens(session, user_id)
    if not fb_dtsg or not lsd:
        print("[!] Không lấy được fb_dtsg hoặc lsd token. Vui lòng kiểm tra lại Cookie.")
        sys.exit(1)

    # 5. Thử đổi tên lần 1
    result = update_name(session, user_id, fb_dtsg, jazoest, lsd,
                         first_name, middle_name, last_name, full_name)
    print("\n[<] Phản hồi từ Facebook:")
    print(json.dumps(result, indent=2, ensure_ascii=False))

    # Kiểm tra xem có đổi thành công ngay không (Nick Trust)
    if "data" in result and result.get("data", {}).get("fxim_identity_for_id"):
        print("\n" + "=" * 70)
        print(f"[SUCCESS] ĐỔI TÊN THÀNH CÔNG THẲNG MÀ KHÔNG BỊ HỎI 2FA!")
        print(f"          Tên mới: {full_name}")
        print("=" * 70)
        return

    # Nếu bị vướng bảo mật (Checkpoint / 2FA / SECURED_ACTION)
    print("\n[!] Tài khoản bị kích hoạt bảo mật (SECURED_ACTION / 2FA)!")
    
    # Bổ sung encrypted_context nếu response trả về hoặc yêu cầu nhập
    if not encrypted_context:
        # Tìm trong result nếu có
        res_str = json.dumps(result)
        m_ctx = re.search(r'"encryptedContext":"([^"]+)"', res_str)
        if m_ctx:
            encrypted_context = m_ctx.group(1)

    if not encrypted_context:
        encrypted_context = input("\n[?] Nhập encryptedContext (lấy từ req5/req6 nếu có): ").strip()

    if not masked_email:
        res_str = json.dumps(result)
        m_em = re.search(r'"maskedContactPoint":"([^"]+)"', res_str) or re.search(r'e\*{5,10}\w+@\w+\.\w+', res_str)
        if m_em:
            masked_email = m_em.group(0)
        else:
            masked_email = input("[?] Nhập email ẩn (ví dụ: e**********8@hotmail.com): ").strip()

    # Kích hoạt gửi OTP về Email
    send_res = send_verification_code(session, user_id, fb_dtsg, jazoest, lsd,
                                      encrypted_context, masked_email)
    print("[<] Phản hồi gửi mã:")
    print(json.dumps(send_res, indent=2, ensure_ascii=False))

    # Người dùng nhập mã OTP nhận được
    print("\n" + "-" * 70)
    otp_code = input("[?] Vui lòng nhập mã OTP 8 chữ số nhận từ Email: ").strip()
    print("-" * 70)

    # Xác thực mã OTP
    val_res = validate_otp_code(session, user_id, fb_dtsg, jazoest, lsd,
                                encrypted_context, masked_email, otp_code)
    print("[<] Phản hồi xác thực mã:")
    print(json.dumps(val_res, indent=2, ensure_ascii=False))

    # Gửi lại lệnh đổi tên sau khi session đã được cấp phép
    print("\n[*] Đang gửi lại lệnh đổi tên sau khi xác minh OTP...")
    final_res = update_name(session, user_id, fb_dtsg, jazoest, lsd,
                            first_name, middle_name, last_name, full_name)
    print("[<] Phản hồi chốt đổi tên:")
    print(json.dumps(final_res, indent=2, ensure_ascii=False))

    if "data" in final_res and final_res.get("data", {}).get("fxim_identity_for_id"):
        print("\n" + "=" * 70)
        print(f"[SUCCESS] ĐỔI TÊN THÀNH CÔNG SAU KHI XÁC MINH 2FA!")
        print(f"          Tên mới: {full_name}")
        print("=" * 70)
    else:
        print("\n[!] Đổi tên thất bại. Vui lòng xem phản hồi chi tiết ở trên.")

if __name__ == "__main__":
    main()
