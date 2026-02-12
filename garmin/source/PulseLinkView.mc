using Toybox.WatchUi;
using Toybox.Graphics;
using Toybox.Application;
using Toybox.Lang;

class PulseLinkView extends WatchUi.View {

    function initialize() {
        View.initialize();
    }

    function onLayout(dc) {
    }

    function onUpdate(dc) {
        dc.setColor(Graphics.COLOR_BLACK, Graphics.COLOR_BLACK);
        dc.clear();

        var app = Application.getApp();
        var cx = dc.getWidth() / 2;
        var cy = dc.getHeight() / 2;

        if (app.isAlarming) {
            drawAlarmScreen(dc, cx, cy);
        } else {
            drawMainScreen(dc, app, cx, cy);
        }
    }

    private function drawAlarmScreen(dc, cx, cy) {
        // Alarm title
        dc.setColor(Graphics.COLOR_RED, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            cx, cy - 50,
            Graphics.FONT_LARGE,
            "ALARM!",
            Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER
        );

        // Dismiss instruction
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            cx, cy + 40,
            Graphics.FONT_SMALL,
            "Press any button",
            Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER
        );
        dc.drawText(
            cx, cy + 65,
            Graphics.FONT_SMALL,
            "to dismiss",
            Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER
        );
    }

    private function drawMainScreen(dc, app, cx, cy) {
        // App name
        dc.setColor(0x4FC3F7, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            cx, cy - 90,
            Graphics.FONT_MEDIUM,
            "PulseLink",
            Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER
        );

        if (app.alarmHour != null && app.alarmMinute != null) {
            // Alarm time in large digits
            var timeStr = Lang.format("$1$:$2$", [
                app.alarmHour.format("%02d"),
                app.alarmMinute.format("%02d")
            ]);

            dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
            dc.drawText(
                cx, cy - 10,
                Graphics.FONT_NUMBER_HOT,
                timeStr,
                Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER
            );

            // Label
            dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
            dc.drawText(
                cx, cy + 70,
                Graphics.FONT_SMALL,
                "Next Alarm",
                Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER
            );
        } else {
            // No alarm
            dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
            dc.drawText(
                cx, cy,
                Graphics.FONT_MEDIUM,
                "No alarm set",
                Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER
            );
        }

        // Status footer — show whether the background event is armed
        var statusText;
        var statusColor;
        if (app.alarmScheduled) {
            statusText = "Alarm scheduled";
            statusColor = 0x4CAF50;
        } else {
            statusText = "Waiting for phone...";
            statusColor = Graphics.COLOR_DK_GRAY;
        }
        dc.setColor(statusColor, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            cx, cy + 110,
            Graphics.FONT_XTINY,
            statusText,
            Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER
        );
    }
}
