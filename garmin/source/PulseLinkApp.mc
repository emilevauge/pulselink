using Toybox.Application;
using Toybox.Application.Storage;
using Toybox.Background;
using Toybox.Communications as Comm;
using Toybox.WatchUi;
using Toybox.Attention;
using Toybox.Timer;
using Toybox.Time;
using Toybox.Time.Gregorian;
using Toybox.Lang;
using Toybox.System;

class PulseLinkApp extends Application.AppBase {

    var alarmHour = null;
    var alarmMinute = null;
    var alarmScheduled = false;
    var isAlarming = false;
    var vibeTimer = null;

    function initialize() {
        AppBase.initialize();
    }

    function onStart(state) {
        Comm.registerForPhoneAppMessages(method(:onPhoneMessage));

        // Restore persisted alarm on app start
        alarmHour = Storage.getValue("alarmHour");
        alarmMinute = Storage.getValue("alarmMinute");
        alarmScheduled = (Storage.getValue("alarmScheduled") != null);
    }

    function getInitialView() {
        return [new PulseLinkView(), new PulseLinkDelegate()];
    }

    // Glance view stays loaded in the widget loop, keeping the app
    // resident so phone messages and onBackgroundData() are delivered.
    (:glance)
    function getGlanceView() {
        return [new PulseLinkGlanceView()];
    }

    // Called by the system when our background service delegate
    // exits via Background.exit(data). Runs in main app context
    // with full API access (Attention, WatchUi, etc.).
    function onBackgroundData(data) {
        if (data instanceof Lang.Dictionary && data["trigger"] != null) {
            isAlarming = true;
            alarmScheduled = false;
            Storage.deleteValue("alarmScheduled");
            startAlarm();
            WatchUi.requestUpdate();
        }
    }

    // Return the background service delegate for temporal events
    function getServiceDelegate() {
        return [new PulseLinkServiceDelegate()];
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
                scheduleTemporalEvent(alarmHour, alarmMinute);
                WatchUi.requestUpdate();
            } else if (type != null && type.equals("alarm_trigger")) {
                // Backup trigger from phone (in case temporal event
                // couldn't fire, e.g. app was killed by the system)
                if (!isAlarming) {
                    isAlarming = true;
                    alarmScheduled = false;
                    startAlarm();
                    WatchUi.requestUpdate();
                }
            } else if (type != null && type.equals("alarm_clear")) {
                alarmHour = null;
                alarmMinute = null;
                isAlarming = false;
                alarmScheduled = false;
                stopVibeTimer();
                cancelTemporalEvent();
                WatchUi.requestUpdate();
            }
        }
    }

    // Schedule a background temporal event at the given hour:minute.
    // The system will wake PulseLinkServiceDelegate at that time,
    // even if the app is not in the foreground.
    function scheduleTemporalEvent(hour, minute) {
        var now = Time.now();
        var nowInfo = Gregorian.info(now, Time.FORMAT_SHORT);

        var alarmMoment = Gregorian.moment({
            :year => nowInfo.year,
            :month => nowInfo.month,
            :day => nowInfo.day,
            :hour => hour,
            :minute => minute,
            :second => 0
        });

        // If the alarm time already passed today, schedule for tomorrow
        if (alarmMoment.value() <= now.value()) {
            alarmMoment = alarmMoment.add(new Time.Duration(86400));
        }

        // Only one temporal event can be active at a time;
        // calling this again replaces the previous one.
        Background.registerForTemporalEvent(alarmMoment);
        alarmScheduled = true;

        // Persist so we can restore state if the app restarts
        Storage.setValue("alarmHour", hour);
        Storage.setValue("alarmMinute", minute);
        Storage.setValue("alarmScheduled", true);
    }

    function cancelTemporalEvent() {
        Background.deleteTemporalEvent();
        alarmScheduled = false;
        Storage.deleteValue("alarmHour");
        Storage.deleteValue("alarmMinute");
        Storage.deleteValue("alarmScheduled");
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
