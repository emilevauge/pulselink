using Toybox.System;
using Toybox.Background;

// Runs in the background context when a temporal event fires.
// Has limited API access — cannot use Attention, WatchUi, etc.
// Passes data back to the main app via Background.exit().
(:background)
class PulseLinkServiceDelegate extends System.ServiceDelegate {

    function initialize() {
        ServiceDelegate.initialize();
    }

    function onTemporalEvent() {
        // Signal the main app that the alarm time has been reached
        Background.exit({"trigger" => true});
    }
}
