
var exec = require('cordova/exec');

var PLUGIN_NAME = 'Consent';

var Consent = {
	verifyConsent: function(publisherIds, privacyPolicyUrl, showProVersionOption, isDebug, cb, cbError) {
		exec(cb, cbError, PLUGIN_NAME, 'verifyConsent', [publisherIds, privacyPolicyUrl, showProVersionOption, isDebug]);
	},
	changeConsentDecision: function(privacyPolicyUrl, showProVersionOption, cb, cbError) {
		exec(cb, cbError, PLUGIN_NAME, 'changeConsentDecision', [privacyPolicyUrl, showProVersionOption]);
	}
};

module.exports = Consent;
