package com.phicdy.redraft.shared.ui.main

import com.phicdy.redraft.shared.llm.LlmError

internal const val UNEXPECTED_ERROR_MESSAGE: String = "予期しないエラーが発生しました。もう一度お試しください"

/** A short Japanese message for the user. Details meant for developers are left out. */
internal fun LlmError.toUserMessage(): String = when (this) {
    LlmError.MissingApiKey -> "APIキーが設定されていません。設定画面から登録してください"
    is LlmError.Unauthorized -> "APIキーが無効です。設定画面でAPIキーを確認してください"
    is LlmError.RateLimited -> if (retryAfterSeconds != null) {
        "リクエストが多すぎます。${retryAfterSeconds}秒後にもう一度お試しください"
    } else {
        "リクエストが多すぎます。しばらく待ってからもう一度お試しください"
    }

    is LlmError.ServerError -> "サーバーでエラーが発生しました（HTTP $status）。しばらく待ってからもう一度お試しください"
    is LlmError.HttpError -> "リクエストに失敗しました（HTTP $status）"
    LlmError.Timeout -> "応答がタイムアウトしました。もう一度お試しください"
    is LlmError.Network -> "サーバーに接続できませんでした。ネットワーク接続を確認してください"
    is LlmError.UnexpectedResponse -> "サーバーから想定外の応答が返されました"
    is LlmError.Refused -> if (message.isBlank()) {
        "モデルが回答を拒否しました"
    } else {
        "モデルが回答を拒否しました: $message"
    }

    is LlmError.Incomplete -> "出力が途中で終了しました。入力を短くしてもう一度お試しください"
    is LlmError.GenerationFailed -> "生成に失敗しました。もう一度お試しください"
    is LlmError.InvalidOutput -> "結果を読み取れませんでした。もう一度お試しください"
}
