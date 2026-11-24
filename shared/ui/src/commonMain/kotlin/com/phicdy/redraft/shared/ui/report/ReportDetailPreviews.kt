package com.phicdy.redraft.shared.ui.report

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.phicdy.redraft.shared.data.report.WeeklyReport
import com.phicdy.redraft.shared.ui.theme.RedraftTheme

@Composable
private fun ReportDetailPreview(report: WeeklyReport) {
    RedraftTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ReportDetail(report = report, timeZone = previewTimeZone, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview
@Composable
internal fun ReportDetailCompletedPreview() {
    ReportDetailPreview(sampleReports[1])
}

@Preview
@Composable
internal fun ReportDetailPartialPreview() {
    ReportDetailPreview(sampleReports[0])
}
