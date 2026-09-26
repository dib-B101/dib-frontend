package com.ssafy.dib.feature.main

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebSettings
import android.webkit.WebResourceResponse
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ssafy.dib.BuildConfig
import com.ssafy.dib.core.time.formatServerTime
import com.ssafy.dib.core.ui.DibMainTab
import com.ssafy.dib.core.ui.DibDialog
import com.ssafy.dib.core.ui.DibDialogConfirmButton
import com.ssafy.dib.core.ui.DibDialogDismissButton
import com.ssafy.dib.core.ui.DibPullToRefreshBox
import com.ssafy.dib.domain.payment.PaymentMethod
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import org.json.JSONObject

@Composable
fun PaymentMethodsScreen(
    paymentMethod: PaymentMethod?,
    isLoading: Boolean,
    errorMessage: String?,
    actionLoading: Boolean,
    actionMessage: String?,
    actionError: String?,
    tossClientKey: String,
    customerKey: String,
    onRetry: () -> Unit,
    onRegister: (authKey: String, customerKey: String) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onTabSelected: (DibMainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBillingAuth by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var billingAuthError by remember { mutableStateOf<String?>(null) }
    var billingAgreementAccepted by remember { mutableStateOf(false) }

    SettingsScaffold("결제수단 관리", onBack, onTabSelected, modifier) { padding ->
        DibPullToRefreshBox(isRefreshing = isLoading, onRefresh = onRetry, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            item {
                Column(
                    Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, Colors.Border, RoundedCornerShape(16.dp)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("자동결제 카드", color = Colors.Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("낙찰되면 등록한 카드로 결제를 요청해요.", color = Colors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
            actionMessage?.let { message ->
                item { StatusMessage("✓ $message", Color(0xFFE8FAF5), Color(0xFF27806E)) }
            }
            (actionError ?: billingAuthError)?.let { message ->
                item { StatusMessage(message, Color(0xFFFFEEF0), Colors.Urgent) }
            }
            when {
                isLoading -> item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = Colors.Navy)
                    }
                }
                errorMessage != null -> item {
                    Column(
                        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
                            .border(1.dp, Colors.Border, RoundedCornerShape(14.dp)).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(errorMessage, color = Colors.Muted, fontSize = 13.sp)
                        OutlinedButton(onClick = onRetry) { Text("다시 불러오기") }
                    }
                }
                paymentMethod != null -> item {
                    Column(
                        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, Colors.Border, RoundedCornerShape(16.dp)).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(46.dp).background(Color(0xFFDDF8F0), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                                Text("CARD", color = Colors.Navy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(paymentMethod.cardCompany ?: "등록 카드", color = Colors.Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(paymentMethod.cardNumber ?: "카드 번호 확인 불가", color = Colors.Muted, fontSize = 13.sp)
                            }
                        }
                        formatServerTime(paymentMethod.createdAt)?.let { registeredAt ->
                            Text("등록일 $registeredAt", color = Colors.Muted, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            enabled = !actionLoading,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(13.dp)
                        ) { Text("등록 카드 삭제", color = Colors.Urgent, fontWeight = FontWeight.Bold) }
                    }
                }
                else -> {
                    item {
                        Column(
                            Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp))
                                .border(1.dp, Colors.Border, RoundedCornerShape(16.dp)).padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("등록된 카드가 없어요", color = Colors.Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("낙찰 직후 자동결제를 위해 카드를 미리 등록해주세요.", color = Colors.Muted, fontSize = 12.sp)
                        }
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(14.dp))
                                .border(1.dp, Colors.Border, RoundedCornerShape(14.dp))
                                .clickable { billingAgreementAccepted = !billingAgreementAccepted }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = billingAgreementAccepted,
                                onCheckedChange = null
                            )
                            Column(Modifier.padding(start = 4.dp)) {
                                Text("자동결제 카드 등록에 동의해요 (필수)", color = Colors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("낙찰되면 등록 카드로 낙찰 금액 전액을 자동결제해요.", color = Colors.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = { billingAuthError = null; showBillingAuth = true },
                            enabled = billingAgreementAccepted && !actionLoading && tossClientKey.isNotBlank() && customerKey.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Colors.Navy)
                        ) {
                            if (actionLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text(if (billingAgreementAccepted) "카드 등록하기" else "필수 동의 후 등록하기", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (tossClientKey.isBlank()) {
                        item { Text("개발 환경에 DIB_TOSS_CLIENT_KEY가 설정되지 않았어요.", color = Colors.Urgent, fontSize = 11.sp) }
                    }
                }
            }
            }
        }
    }

    if (showBillingAuth) {
        TossBillingAuthDialog(
            clientKey = tossClientKey,
            customerKey = customerKey,
            onSuccess = { authKey, returnedCustomerKey ->
                showBillingAuth = false
                onRegister(authKey, returnedCustomerKey)
            },
            onFailure = {
                showBillingAuth = false
                billingAuthError = it
            },
            onDismiss = { showBillingAuth = false }
        )
    }
    if (showDeleteConfirmation) {
        DibDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = "등록 카드를 삭제할까요?",
            text = { Text("진행 중인 입찰이나 결제에 사용 중인 카드는 서버 정책에 따라 삭제가 제한될 수 있어요.", color = Colors.Muted, fontSize = 13.sp, lineHeight = 19.sp) },
            confirmButton = { DibDialogConfirmButton("삭제", { showDeleteConfirmation = false; onDelete() }, destructive = true) },
            dismissButton = { DibDialogDismissButton({ showDeleteConfirmation = false }) }
        )
    }
}

@Composable
private fun StatusMessage(message: String, background: Color, foreground: Color) {
    Text(message, Modifier.fillMaxWidth().background(background, RoundedCornerShape(12.dp)).padding(14.dp), color = foreground, fontSize = 12.sp)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TossBillingAuthDialog(
    clientKey: String,
    customerKey: String,
    onSuccess: (String, String) -> Unit,
    onFailure: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val webView = remember {
        // chrome://inspect 로 이 WebView 의 네트워크·콘솔을 직접 볼 수 있게 (디버그 빌드 한정)
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            // 토스 인증 다음 단계(비밀번호 입력)는 다른 도메인의 iframe·세션을 탄다.
            // WebView 는 서드파티 쿠키를 기본 차단하므로 이걸 켜지 않으면 세션이 끊겨 회색 빈 화면이 된다
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = true
            // setSupportMultipleWindows 는 false(기본) 로 둔다. false 면 window.open 을 WebView 가
            // 현재 창에서 그대로 열어주므로, 직접 onCreateWindow 로 URL 을 가로채다가 토스 내부 요청까지
            // 메인 화면에 끌어오는 사고를 막을 수 있다
            // 토스 SDK 가 실제로 던지는 에러를 Logcat 으로 본다 (필터: TossWebView)
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    Log.w(TOSS_LOG_TAG, "${message.messageLevel()} ${message.message()} @${message.sourceId()}:${message.lineNumber()}")
                    return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    if ((uri.scheme == "http" || uri.scheme == "https") && uri.host == CALLBACK_HOST) {
                        when (uri.path) {
                            SUCCESS_PATH -> {
                                val authKey = uri.getQueryParameter("authKey").orEmpty()
                                val returnedCustomerKey = uri.getQueryParameter("customerKey").orEmpty()
                                if (authKey.isNotBlank() && returnedCustomerKey == customerKey) onSuccess(authKey, returnedCustomerKey)
                                else onFailure("카드 인증 결과를 확인할 수 없어요.")
                            }
                            FAIL_PATH -> {
                                val code = uri.getQueryParameter("code").orEmpty()
                                val message = uri.getQueryParameter("message") ?: "카드 인증을 완료하지 못했어요."
                                Log.w(TOSS_LOG_TAG, "billing auth failed code=$code message=$message")
                                onFailure(if (code.isBlank()) message else "$message ($code)")
                            }
                        }
                        return true
                    }
                    if (uri.scheme == "http" || uri.scheme == "https") return false
                    return runCatching {
                        val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                        context.startActivity(intent)
                        true
                    }.getOrElse {
                        onFailure("카드 인증 앱을 열 수 없어요. 설치 상태를 확인해주세요.")
                        true
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    Log.w(TOSS_LOG_TAG, "page started $url")
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    Log.w(TOSS_LOG_TAG, "ssl error ${error?.primaryError} ${error?.url}")
                    handler?.cancel()
                }

                // 토스 SDK 가 UNKNOWN 을 던질 때 실제로 어떤 요청이 실패했는지 본다
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    Log.w(TOSS_LOG_TAG, "resource error ${request?.url} : ${error?.errorCode} ${error?.description}")
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    Log.w(TOSS_LOG_TAG, "http error ${errorResponse?.statusCode} ${request?.url}")
                }
            }
            loadDataWithBaseURL(BASE_URL, billingAuthHtml(clientKey, customerKey), "text/html", "UTF-8", null)
        }
    }
    DisposableEffect(webView) { onDispose { webView.stopLoading(); webView.destroy() } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        // safeDrawingPadding: 상태바·내비게이션바에 더해 키보드(ime)까지 피한다.
        // 이게 없으면 카드번호 다음 단계 입력칸이 키보드에 가려져 화면이 멈춘 것처럼 보인다
        Column(Modifier.fillMaxSize().background(Color.White).safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("닫기", color = Colors.Navy) }
                Text("카드 인증", Modifier.padding(start = 8.dp), color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
        }
    }
}

private fun billingAuthHtml(clientKey: String, customerKey: String): String {
    val quotedClientKey = JSONObject.quote(clientKey)
    val quotedCustomerKey = JSONObject.quote(customerKey)
    val successUrl = JSONObject.quote("${BASE_URL.trimEnd('/')}$SUCCESS_PATH")
    val failUrl = JSONObject.quote("${BASE_URL.trimEnd('/')}$FAIL_PATH")
    return """
        <!doctype html><html lang="ko"><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <script src="https://js.tosspayments.com/v2/standard"></script>
        <style>body{font-family:sans-serif;margin:0;padding:28px;color:#1d2942}button{width:100%;height:54px;border:0;border-radius:14px;background:#17233f;color:white;font-size:16px;font-weight:700}.box{margin-top:28px;padding:20px;border:1px solid #e1e5ea;border-radius:16px}p{color:#687386;line-height:1.6;font-size:14px}</style></head>
        <body><h2>자동결제 카드 등록</h2><p>카드 인증을 완료하면 DIB 서버가 빌링키를 안전하게 발급받아 저장합니다.</p><div class="box"><button id="register">토스페이먼츠에서 카드 인증</button></div>
        <script>
        console.log('origin=' + location.origin + ' clientKeyPrefix=' + $quotedClientKey.slice(0, 8) + ' clientKeyLen=' + $quotedClientKey.length + ' customerKey=' + $quotedCustomerKey);
        window.addEventListener('error', function(ev){ console.log('window error: ' + (ev && ev.message)); });
        document.getElementById('register').addEventListener('click', async function(){
          try {
            if (typeof TossPayments !== 'function') throw new Error('토스 SDK 스크립트를 불러오지 못했습니다.');
            const tossPayments = TossPayments($quotedClientKey);
            const payment = tossPayments.payment({customerKey:$quotedCustomerKey});
            await payment.requestBillingAuth({method:'CARD',successUrl:$successUrl,failUrl:$failUrl,windowTarget:'self'});
          } catch(e) {
            console.log('requestBillingAuth error name=' + (e && e.name) + ' code=' + (e && e.code) + ' message=' + (e && e.message));
            location.href = $failUrl + '?code=' + encodeURIComponent((e && e.code) || 'SDK_ERROR') + '&message=' + encodeURIComponent((e && e.message) || '카드 인증을 시작하지 못했어요.');
          }
        });
        </script></body></html>
    """.trimIndent()
}

private const val TOSS_LOG_TAG = "TossWebView"

// 토스는 successUrl / failUrl 의 형식을 검증한다. dib.local 처럼 실존하지 않는 TLD 는
// INCORRECT_SUCCESS_URL_FORMAT 으로 거절되므로 토스 문서 예시와 같은 localhost 를 쓴다.
// 이 주소로 실제 요청이 나가지는 않는다 — 카드 인증 후 리다이렉트를 WebViewClient 가 가로채서 authKey 만 꺼낸다.
private const val CALLBACK_HOST = "localhost"
private const val BASE_URL = "http://$CALLBACK_HOST:8080/"
private const val SUCCESS_PATH = "/payment-method/success"
private const val FAIL_PATH = "/payment-method/fail"
