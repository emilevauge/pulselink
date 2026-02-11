using Toybox.Application;
using Toybox.Communications as Comm;
using Toybox.WatchUi;
using Toybox.Attention;
using Toybox.Timer;
using Toybox.Lang;
using Toybox.System;

class PulseLinkApp extends Application.AppBase {

    var alarmHour = null;
    var alarmMinute = null;
    var isAlarming = false;
    var vibeTimer = null;

    function initialize() {
        AppBase.initialize();
    }

    function onStart(state) {
        Comm.registerForPhoneAppMessages(method(:onPhoneMessage));
    }

    function getInitialView() {
        return [new PulseLinkView(), new PulseLinkDelegate()];
    }

    function onPhoneMessage(msg) {
        var data = msg.data;
        if (data == null) {
            return;
        }

        if (data instanceof Lang.Dictionary) {
            var type = data["type"];

            if (type != null && type.equals("alarm_sync")) {
                alarmHour = data["hour"];
                alarmMinute = data["minute"];
                isAlarming = false;
                stopVibeTimer();
                WatchUi.requestUpdate();
            } else if (type != null && type.equals("alarm_trigger")) {
                isAlarming = true;
                startAlarm();
                WatchUi.requestUpdate();
            } else if (type != null && type.equals("alarm_clear")) {
                alarmHour = null;
                alarmMinute = null;
                isAlarming = false;
                stopVibeTimer();
                WatchUi.requestUpdate();
            }
        }
    }

    function startAlarm() {
        doVibrate();
        // Repeat vibration every 5 seconds until dismissed
        vibeTimer = new Timer.Timer();
        vibeTimer.start(method(:onVibeTimer), 5000, true);
    }

    function onVibeTimer() {
        if (isAlarming) {
            doVibrate();
        } else {
            stopVibeTimer();
        }
    }

    function doVibrate() {
        if (Attention has :vibrate) {
            var vibePattern = [
                new Attention.VibeProfile(100, 1000),
                new Attention.VibeProfile(0, 400),
                new Attention.VibeProfile(100, 1000),
                new Attention.VibeProfile(0, 400),
                new Attention.VibeProfile(100, 1000)
            ];
            Attention.vibrate(vibePattern);
        }

        if (Attention has :playTone) {
            Attention.playTone(Attention.TONE_ALARM);
        }

        if (Attention has :backlight) {
            Attention.backlight(true);
        }
    }

    function stopAlarm() {
        isAlarming = false;
        stopVibeTimer();
        WatchUi.requestUpdate();
    }

    function stopVibeTimer() {
        if (vibeTimer != null) {
            vibeTimer.stop();
            vibeTimer = null;
        }
    }

    function onStop(state) {
        stopVibeTimer();
    }
}
