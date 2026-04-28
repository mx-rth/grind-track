@file:Suppress("RestrictedApi")

package intellij.kmm.settings.grind_track.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import intellij.kmm.settings.grind_track.MainActivity
import intellij.kmm.settings.grind_track.R
import intellij.kmm.settings.grind_track.core.data.ProgressRepository
import intellij.kmm.settings.grind_track.core.data.TodayStatus
import intellij.kmm.settings.grind_track.core.data.WorkoutWidgetData
import kotlinx.coroutines.flow.first
import org.koin.mp.KoinPlatformTools

private val colorSurface = ColorProvider(R.color.widget_surface)
private val colorPrimary = ColorProvider(R.color.widget_primary)
private val colorMuted   = ColorProvider(R.color.widget_muted)
private val colorGreen   = ColorProvider(R.color.widget_green)

class WorkoutWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = runCatching {
            val repo = KoinPlatformTools.defaultContext().get().get<ProgressRepository>()
            repo.observeWidgetData().first()
        }.getOrElse { WorkoutWidgetData(streak = 0, todayStatus = TodayStatus.RestDay) }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .background(colorSurface)
                    .cornerRadius(16.dp)
                    .padding(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "🔥 ${data.streak}",
                    style = TextStyle(
                        color = colorPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                )
                Text(
                    text = "day streak",
                    style = TextStyle(
                        color = colorMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(10.dp))
                Text(
                    text = when (data.todayStatus) {
                        TodayStatus.RestDay -> "Rest day"
                        TodayStatus.Pending -> "Workout pending"
                        TodayStatus.Complete -> "Done for today ✓"
                    },
                    style = TextStyle(
                        color = when (data.todayStatus) {
                            TodayStatus.RestDay -> colorMuted
                            TodayStatus.Pending -> colorPrimary
                            TodayStatus.Complete -> colorGreen
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}
