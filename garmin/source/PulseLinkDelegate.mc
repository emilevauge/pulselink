using Toybox.WatchUi;
using Toybox.Application;

class PulseLinkDelegate extends WatchUi.BehaviorDelegate {

    function initialize() {
        BehaviorDelegate.initialize();
    }

    // START/STOP button or screen tap
    function onSelect() {
        return dismissIfAlarming();
    }

    // Touchscreen tap
    function onTap(clickEvent) {
        return dismissIfAlarming();
    }

    // BACK/LAP button
    function onBack() {
        var app = Application.getApp();
        if (app.isAlarming) {
            app.stopAlarm();
            return true;
        }
        // Default behavior: exit the app
        return false;
    }

    // UP button
    function onPreviousPage() {
        return dismissIfAlarming();
    }

    // DOWN button
    function onNextPage() {
        return dismissIfAlarming();
    }

    private function dismissIfAlarming() {
        var app = Application.getApp();
        if (app.isAlarming) {
            app.stopAlarm();
            return true;
        }
        return false;
    }
}
