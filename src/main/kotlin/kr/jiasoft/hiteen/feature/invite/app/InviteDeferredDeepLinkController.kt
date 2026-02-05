package kr.jiasoft.hiteen.feature.invite.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 초대코드 공유용 디퍼드 딥링크 엔드포인트.
 *
 * NOTE
 * - "진짜" 디퍼드 딥링크(설치 후 파라미터 복원)는 플랫폼/SDK(Branch, AppsFlyer, Play Install Referrer 등)가 필요합니다.
 * - 이 컨트롤러는 서버 측에서 공유 URL을 제공하고:
 *   - 설치된 경우: 앱 링크(커스텀 스킴)로 열기 시도
 *   - 미설치/실패: 스토어로 폴백
 * - 앱은 최종적으로 회원가입 API에 inviteCode를 넣어야 합니다.
 */
@Tag(name = "Invite", description = "초대코드/추천인")
@RestController
@RequestMapping("/r")
class InviteDeferredDeepLinkController(
    private val inviteDeferredService: InviteDeferredService,
    @Value("\${app.deeplink.invite.app-scheme:hiteen://invite}")
    private val inviteAppScheme: String,
    @Value("\${app.deeplink.invite.android-store:https://play.google.com/store/apps/details?id=kr.jiasoft.hiteen}")
    private val androidStoreUrl: String,
    @Value("\${app.deeplink.invite.ios-store:https://apps.apple.com/app/id0000000000}")
    private val iosStoreUrl: String,
) {

    @Operation(summary = "초대코드 디퍼드 딥링크", description = "초대코드 공유 URL. Android는 Install Referrer를 통해 설치 후 토큰을 복원할 수 있습니다.")
    @GetMapping("/invite")
    suspend fun invite(@RequestParam("code") code: String): ResponseEntity<String> {
        val appUrl = "$inviteAppScheme?code=${encode(code)}"

        // ✅ Android: Install Referrer에 담기 위해 server-side 토큰 발급 후 스토어 URL에 붙인다
        // - 앱은 설치 후 Install Referrer에서 token을 읽고 /api/invite/deferred/resolve 로 code를 복원
        val token = inviteDeferredService.issueToken(code)
        val androidStoreWithReferrer = buildAndroidStoreUrl(androidStoreUrl, token)

//        val html = """
//            <!doctype html>
//            <html lang=\"ko\">
//              <head>
//                <meta charset=\"utf-8\" />
//                <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
//                <title>HiTeen Invite</title>
//                <style>
//                  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 24px; }
//                  .card { max-width: 520px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 12px; }
//                  .title { font-size: 18px; font-weight: 700; margin-bottom: 6px; }
//                  .desc { color: #555; line-height: 1.5; margin: 0 0 14px; }
//                  .row { display: flex; gap: 10px; flex-wrap: wrap; }
//                  button { appearance: none; border: 0; padding: 12px 14px; border-radius: 10px; background: #111; color: #fff; font-weight: 700; }
//                  .secondary { background: #efefef; color: #111; }
//                  .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; background: #f7f7f7; padding: 10px 12px; border-radius: 10px; word-break: break-all; }
//                  .hint { font-size: 12px; color: #777; margin-top: 10px; }
//                  .ok { color: #0a7; font-size: 12px; margin-top: 8px; display: none; }
//                </style>
//              </head>
//              <body>
//                <div class=\"card\">
//                  <div class=\"title\">초대 링크를 여는 중…</div>
//                  <p class=\"desc\">앱이 설치되어 있으면 앱으로 이동합니다. 설치되어 있지 않으면 스토어로 이동합니다.</p>
//
//                  <div class=\"mono\" id=\"inviteCode\">${escapeHtml(code)}</div>
//                  <div class=\"hint\">
//                    iPhone에서 설치 후 자동 적용이 안 되면, 위 코드를 가입 화면에서 직접 입력할 수 있어요.
//                  </div>
//                  <div class=\"row\" style=\"margin-top: 12px;\">
//                    <button id=\"copyBtn\" type=\"button\">초대코드 복사</button>
//                    <button id=\"openStoreBtn\" class=\"secondary\" type=\"button\">스토어 열기</button>
//                  </div>
//                  <div id=\"copied\" class=\"ok\">복사 완료! 앱 설치 후 가입 화면에서 붙여넣기해 주세요.</div>
//                </div>
//                 <script>
//                   (function () {
//                     var ua = (navigator.userAgent || '').toLowerCase();
//                     var isAndroid = ua.indexOf('android') !== -1;
//                     var isIOS = /iphone|ipad|ipod/.test(ua);
//
//                     var appUrl = ${jsString(appUrl)};
//                     var storeUrl = isAndroid ? ${jsString(androidStoreWithReferrer)} : (isIOS ? ${jsString(iosStoreUrl)} : ${jsString(androidStoreWithReferrer)});
//
//                    var code = ${jsString(code)};
//                    var copyBtn = document.getElementById('copyBtn');
//                    var openStoreBtn = document.getElementById('openStoreBtn');
//                    var copied = document.getElementById('copied');
//
//                    function copyText(text) {
//                      if (navigator.clipboard && navigator.clipboard.writeText) {
//                        return navigator.clipboard.writeText(text);
//                      }
//                      // fallback
//                      var ta = document.createElement('textarea');
//                      ta.value = text;
//                      ta.style.position = 'fixed';
//                      ta.style.opacity = '0';
//                      document.body.appendChild(ta);
//                      ta.focus(); ta.select();
//                      try { document.execCommand('copy'); } catch (e) {}
//                      document.body.removeChild(ta);
//                      return Promise.resolve();
//                    }
//
//                    copyBtn && copyBtn.addEventListener('click', function () {
//                      copyText(code).then(function () {
//                        if (copied) copied.style.display = 'block';
//                      });
//                    });
//                    openStoreBtn && openStoreBtn.addEventListener('click', function () {
//                      window.location.href = storeUrl;
//                    });
//
//                     // 설치된 경우 앱 실행 시도
//                     window.location.href = appUrl;
//
//                     // 미설치/실패 시 스토어로 폴백
//                     // - Android: 설치 후 Install Referrer 자동 복원을 위해 자동 이동 유지
//                     // - iOS: 자동 이동하면 사용자가 "복사"를 누를 시간을 잃는다.
//                     //        또한 클립보드 접근은 사용자 제스처가 필요하므로, 버튼으로 이동 유도.
//                     if (!isIOS) {
//                       setTimeout(function () {
//                         window.location.href = storeUrl;
//                       }, 1200);
//                     }
//                   })();
//                 </script>
//               </body>
//             </html>
//         """.trimIndent()

        val html = """
            <!doctype html>
            <html lang="ko">
              <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <title>HiTeen Invite</title>

                <style>
                  * {
                    box-sizing: border-box;
                  }

                  body {
                    margin: 0;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: linear-gradient(135deg, #f6f7fb, #eef1f8);
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    color: #111;
                  }

                  .container {
                    width: 100%;
                    padding: 20px;
                  }

                  .card {
                    max-width: 420px;
                    margin: 0 auto;
                    background: #fff;
                    border-radius: 16px;
                    padding: 28px 24px;
                    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
                    text-align: center;
                  }

                  .title {
                    font-size: 20px;
                    font-weight: 800;
                    margin-bottom: 8px;
                  }

                  .desc {
                    font-size: 14px;
                    color: #555;
                    line-height: 1.6;
                    margin-bottom: 20px;
                  }

                  .code-box {
                    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
                    font-size: 16px;
                    font-weight: 700;
                    letter-spacing: 1px;
                    background: #f5f7fa;
                    border-radius: 12px;
                    padding: 14px;
                    margin-bottom: 12px;
                    word-break: break-all;
                  }

                  .hint {
                    font-size: 12px;
                    color: #777;
                    margin-bottom: 18px;
                  }

                  .buttons {
                    display: flex;
                    gap: 10px;
                    flex-direction: column;
                  }

                  button {
                    appearance: none;
                    border: none;
                    border-radius: 12px;
                    padding: 14px;
                    font-size: 15px;
                    font-weight: 700;
                    cursor: pointer;
                  }

                  .primary {
                    background: #4f46e5;
                    color: #fff;
                  }

                  .secondary {
                    background: #e5e7eb;
                    color: #111;
                  }

                  .ok {
                    display: none;
                    margin-top: 12px;
                    font-size: 13px;
                    color: #059669;
                    font-weight: 600;
                  }

                  .loading {
                    margin-top: 14px;
                    font-size: 12px;
                    color: #888;
                  }
                </style>
              </head>

              <body>
                <div class="container">
                  <div class="card">
                    <div class="title">초대 링크를 여는 중…</div>
                    <p class="desc">
                      앱이 설치되어 있으면 자동으로 앱이 열립니다.<br />
                      설치되어 있지 않으면 스토어로 이동합니다.
                    </p>

                    <div class="code-box" id="inviteCode">${escapeHtml(code)}</div>

                    <div class="hint">
                      iPhone에서 자동 적용이 안 되면,<br />
                      가입 화면에서 이 코드를 직접 입력해 주세요.
                    </div>

                    <div class="buttons">
                      <button id="copyBtn" class="primary" type="button">
                        초대코드 복사
                      </button>
                      <button id="openStoreBtn" class="secondary" type="button">
                        스토어 열기
                      </button>
                    </div>

                    <div id="copied" class="ok">
                      복사 완료! 앱 설치 후 가입 화면에서 붙여넣기해 주세요.
                    </div>

                    <div class="loading">
                      잠시만 기다려 주세요…
                    </div>
                  </div>
                </div>

                <script>
                   (function () {
                     var ua = (navigator.userAgent || '').toLowerCase();
                     var isAndroid = ua.indexOf('android') !== -1;
                     var isIOS = /iphone|ipad|ipod/.test(ua);

                     var appUrl = ${jsString(appUrl)};
                     var storeUrl = isAndroid
                      ? ${jsString(androidStoreWithReferrer)}
                      : (isIOS ? ${jsString(iosStoreUrl)} : ${jsString(androidStoreWithReferrer)});
                     var code = ${jsString(code)};

                    var copyBtn = document.getElementById('copyBtn');
                    var openStoreBtn = document.getElementById('openStoreBtn');
                    var copied = document.getElementById('copied');

                    function copyText(text) {
                      if (navigator.clipboard && navigator.clipboard.writeText) {
                        return navigator.clipboard.writeText(text);
                      }
                      // fallback
                      var ta = document.createElement('textarea');
                      ta.value = text;
                      ta.style.position = 'fixed';
                      ta.style.opacity = '0';
                      document.body.appendChild(ta);
                      ta.focus(); ta.select();
                      try { document.execCommand('copy'); } catch (e) {}
                      document.body.removeChild(ta);
                      return Promise.resolve();
                    }

                    copyBtn && copyBtn.addEventListener('click', function () {
                      copyText(code).then(function () {
                        if (copied) copied.style.display = 'block';
                      });
                    });
                    openStoreBtn && openStoreBtn.addEventListener('click', function () {
                      window.location.href = storeUrl;
                    });

                    // 설치된 경우 앱 실행 시도
                    if (!isIOS) {
                      window.location.href = appUrl;
                    }

                     // 미설치/실패 시 스토어로 폴백
                     // - Android: 설치 후 Install Referrer 자동 복원을 위해 자동 이동 유지
                     // - iOS: 자동 이동하면 사용자가 "복사"를 누를 시간을 잃는다.
                     //        또한 클립보드 접근은 사용자 제스처가 필요하므로, 버튼으로 이동 유도.
                     if (!isIOS) {
//                       setTimeout(function () {
//                         window.location.href = storeUrl;
//                       }, 3000);
                     }
                   })();
                </script>
              </body>
            </html>
        """.trimIndent()

        return ResponseEntity
            .status(HttpStatus.OK)
            .header(
                HttpHeaders.CONTENT_TYPE,
                "${MediaType.TEXT_HTML_VALUE}; charset=UTF-8"
            )

            .body(html)
    }

    private fun escapeHtml(v: String): String = v
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun buildAndroidStoreUrl(base: String, token: String): String {
        // Play Install Referrer는 referrer 파라미터에 URL-encoded 문자열을 넣는다.
        // 여기서는 "t=<token>" 형태로 전달.
        val referrerValue = encode("t=$token")
        val glue = if (base.contains("?")) "&" else "?"
        return "$base${glue}referrer=$referrerValue"
    }

    private fun encode(v: String): String = java.net.URLEncoder.encode(v, Charsets.UTF_8)

    private fun jsString(v: String): String {
        // 매우 단순한 JS string escape (invite code는 제한된 문자라서 충분)
        val escaped = v.replace("\\", "\\\\").replace("\"", "\\\"")
        return "\"$escaped\""
    }
}
