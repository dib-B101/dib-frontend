package com.ssafy.dib.feature.main

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ssafy.dib.domain.member.MemberAddress
import com.ssafy.dib.core.ui.DibDialog
import com.ssafy.dib.core.ui.DibDialogConfirmButton
import com.ssafy.dib.core.ui.DibDialogDismissButton
import com.ssafy.dib.domain.order.OrderAddressInput
import com.ssafy.dib.ui.theme.WireframeColors as Colors

/**
 * 주문 배송지 입력. 결제(PAID) 직후 구매자가 한 번 입력하고, 판매자가 송장을 넣으면 수정할 수 없다.
 * 우편번호·도로명주소는 카카오(다음) 우편번호 서비스에서 고르고, 상세주소·수령인만 직접 입력한다.
 */
@Composable
fun OrderAddressDialog(
    submitting: Boolean,
    errorMessage: String?,
    savedAddresses: List<MemberAddress> = emptyList(),
    defaultReceiverName: String = "",
    onSubmit: (OrderAddressInput) -> Unit,
    onDismiss: () -> Unit
) {
    var zip by remember { mutableStateOf("") }
    var roadAddress by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var receiverName by remember { mutableStateOf(defaultReceiverName) }
    var receiverPhone by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }

    val phoneDigits = receiverPhone.filter { it.isDigit() }
    val canSubmit = !submitting && zip.isNotBlank() && roadAddress.isNotBlank() &&
        receiverName.isNotBlank() && phoneDigits.length in 10..11

    if (searching) {
        PostcodeSearchDialog(
            onSelected = { selectedZip, selectedAddress, _ ->
                zip = selectedZip
                roadAddress = selectedAddress
                searching = false
            },
            onDismiss = { searching = false }
        )
    }

    DibDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = "배송지 입력",
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "판매자가 송장을 등록하기 전까지만 입력할 수 있어요.",
                    color = Colors.Muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                // 마이페이지에 등록해 둔 배송지가 있으면 골라서 바로 채운다
                if (savedAddresses.isNotEmpty()) {
                    Text("저장된 배송지", color = Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    savedAddresses.forEach { saved ->
                        val selected = saved.postalCode == zip && saved.address == roadAddress
                        Column(
                            Modifier.fillMaxWidth()
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Colors.Navy else Colors.NavySoft,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = !submitting) {
                                    zip = saved.postalCode
                                    roadAddress = saved.address
                                }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(saved.name, color = Colors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                listOf(saved.postalCode.takeIf(String::isNotBlank)?.let { "($it)" }, saved.address)
                                    .filterNotNull().joinToString(" "),
                                color = Colors.Muted,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Text("또는 새 주소로 입력", color = Colors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { searching = true },
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (zip.isBlank()) "우편번호 검색" else "우편번호 다시 찾기", color = Colors.Navy, fontWeight = FontWeight.Bold) }
                if (zip.isNotBlank()) {
                    Text("($zip) $roadAddress", color = Colors.Text, fontSize = 13.sp, lineHeight = 19.sp)
                }
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("상세주소 (동/호수)") },
                    enabled = !submitting,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = receiverName,
                    onValueChange = { receiverName = it },
                    label = { Text("받는 사람") },
                    enabled = !submitting,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = receiverPhone,
                    onValueChange = { input -> receiverPhone = input.filter { it.isDigit() || it == '-' }.take(13) },
                    label = { Text("연락처") },
                    placeholder = { Text("01012345678") },
                    enabled = !submitting,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { Text(it, color = Colors.Urgent, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            DibDialogConfirmButton(
                "등록",
                onClick = {
                    onSubmit(
                        OrderAddressInput(
                            zip = zip,
                            address = roadAddress,
                            detail = detail.takeIf(String::isNotBlank),
                            receiverName = receiverName,
                            receiverPhone = phoneDigits
                        )
                    )
                },
                enabled = canSubmit,
                loading = submitting
            )
        },
        dismissButton = { DibDialogDismissButton(onDismiss, enabled = !submitting) }
    )
}

/**
 * 카카오(다음) 우편번호 서비스. JS 위젯이라 WebView 로 띄우고 결과를 JavascriptInterface 로 돌려받는다.
 * 별도 키 발급이 필요 없고 백엔드도 관여하지 않는다.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PostcodeSearchDialog(
    onSelected: (zip: String, address: String, buildingCode: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            // 위젯이 실제로 뭘 못 불러왔는지 본다 (Logcat 필터: PostcodeWebView)
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    Log.w(POSTCODE_LOG_TAG, "${message.messageLevel()} ${message.message()} @${message.sourceId()}:${message.lineNumber()}")
                    return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    Log.w(POSTCODE_LOG_TAG, "page started $url")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.w(POSTCODE_LOG_TAG, "page finished $url")
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    Log.w(POSTCODE_LOG_TAG, "resource error ${request?.url} : ${error?.errorCode} ${error?.description}")
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    Log.w(POSTCODE_LOG_TAG, "http error ${errorResponse?.statusCode} ${request?.url}")
                }
            }
            addJavascriptInterface(PostcodeBridge(onSelected), "DibPostcode")
            // baseUrl 은 위젯이 주소 데이터를 받아오는 도메인과 같게 맞춘다(origin 이 다르면 CORS 로 막힌다).
            // HTML 에 '#' 이나 '%' 를 쓰면 안 된다. WebView 가 data 를 URL 로 해석해서 '#' 에서 문서를 잘라버리기
            // 때문에 스크립트가 통째로 사라지고 흰 화면이 된다. (encoding 을 base64 로 줘도 이 WebView 는 디코딩하지
            // 않고 base64 문자열을 그대로 그린다.) 그래서 셀렉터는 body>div, 높이는 vh 로 쓴다
            loadDataWithBaseURL(POSTCODE_ORIGIN, POSTCODE_HTML, "text/html", "UTF-8", null)
        }
    }
    DisposableEffect(webView) { onDispose { webView.stopLoading(); webView.destroy() } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(Color.White).safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("닫기", color = Colors.Navy) }
                Text("우편번호 검색", Modifier.padding(start = 8.dp), color = Colors.Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
        }
    }
}

private const val POSTCODE_LOG_TAG = "PostcodeWebView"
private const val POSTCODE_ORIGIN = "https://postcode.map.daum.net"

private const val POSTCODE_HTML = """
<!doctype html><html lang="ko"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>html,body{margin:0;padding:0;height:100vh}body>div{height:100vh}</style>
<script src="https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>
</head><body><div id="wrap"></div>
<script>
  window.onerror = function (m, u, l) { console.log('postcode error ' + m + ' @' + u + ':' + l); };
  console.log('daum=' + (typeof daum));
  new daum.Postcode({
    oncomplete: function (data) {
      var addr = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress;
      window.DibPostcode.onComplete(data.zonecode, addr, data.buildingCode || data.bdMgtSn || addr);
    }
  }).embed(document.getElementById('wrap'));
  console.log('embedded wrapHeight=' + document.getElementById('wrap').offsetHeight);
</script>
</body></html>
"""

/**
 * JS 브릿지는 reflection 으로 호출되므로 익명 객체가 아닌 public 클래스여야 안전하다.
 * (release 빌드에서는 @JavascriptInterface 메서드가 난독화로 지워지지 않게 keep 규칙 필요)
 */
class PostcodeBridge(private val onSelected: (String, String, String) -> Unit) {
    // WebView 의 JS 스레드에서 불리므로 메인 스레드로 넘겨야 Compose 상태를 건드릴 수 있다
    @JavascriptInterface
    fun onComplete(zip: String, address: String, buildingCode: String) {
        Handler(Looper.getMainLooper()).post { onSelected(zip, address, buildingCode) }
    }
}
