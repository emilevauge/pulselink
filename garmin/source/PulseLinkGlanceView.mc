using Toybox.WatchUi;
using Toybox.Graphics;
using Toybox.Application;
using Toybox.Lang;

// Glance view shown in the widget loop.
// The system keeps glances loaded in memory, which means
// onBackgroundData() and phone messages are reliably delivered
// even when the user is on their watch face.
(:glance)
class PulseLinkGlanceView extends WatchUi.GlanceView {

    function initialize() {
        GlanceView.initialize();
    }

    function onUpdate(dc) {
        var app = Application.getApp();
        var y = dc.getHeight() / 2;

        if (app.isAlarming) {
            dc.setColor(Graphics.COLOR_RED, Graphics.COLOR_TRANSPARENT);
            dc.drawText(0, y, Graphics.FONT_GLANCE, "ALARM!",
                Graphics.TEXT_JUSTIFY_LEFT | Graphics.TEXT_JUSTIFY_VCENTER);
        } else if (app.alarmHour != null && app.alarmMinute != null) {
            var timeStr = Lang.format("$1$:$2$", [
                app.alarmHour.format("%02d"),
                app.alarmMinute.format("%02d")
            ]);
            dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
            dc.drawText(0, y - 15, Graphics.FONT_GLANCE, "PulseLink",
                Graphics.TEXT_JUSTIFY_LEFT | Graphics.TEXT_JUSTIFY_VCENTER);
            dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
            dc.drawText(0, y + 15, Graphics.FONT_GLANCE_NUMBER,
                timeStr,
                Graphics.TEXT_JUSTIFY_LEFT | Graphics.TEXT_JUSTIFY_VCENTER);
        } else {
            dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
            dc.drawText(0, y, Graphics.FONT_GLANCE, "PulseLink - No alarm",
                Graphics.TEXT_JUSTIFY_LEFT | Graphics.TEXT_JUSTIFY_VCENTER);
        }
    }
}
