import {Injectable, NgZone} from '@angular/core';

export interface ConsentResult {
	consent: "PERSONALIZED" | "NON_PERSONALIZED" | "UNKNOWN" | null;
	isAdFree: boolean;
	hasShownDialog: boolean;
	isNotInEea: boolean; // I know it's bad to reverse formulate, but because google is population "is not in EEA" via exception, i want to default the normal case if not submitted to "user is in EEA"
}

let ngZone;

@Injectable()
export class Consent {

	constructor (
		private zone :NgZone
	) {
		ngZone = this.zone; // save to local variable because the "this" context gets lost in some callbacks of the class methods
	}

	verifyConsent(publisherIds :Array<string>, privacyPolicyUrl :string, showProVersionOption :boolean, isDebug :boolean) :Promise<ConsentResult> {
		if (!window['Consent']) {
			console.warn('Consent plugin not present (verifyConsent)');
			return Promise.reject('Consent plugin not present');
		}

		return new Promise((resolve, reject) =>
			window['Consent'].verifyConsent(publisherIds, privacyPolicyUrl, showProVersionOption, isDebug,
				(value: ConsentResult) => ngZone.run(() => resolve(value)),
				(value: string) => ngZone.run(() => {
					if (value.indexOf("request is not in EEA or unknown") !== -1) {
						let result :ConsentResult = {
							consent: "UNKNOWN",
							isAdFree: false,
							hasShownDialog: false,
							isNotInEea: true
						};
						resolve(result);
					} else {
						reject(value);
					}
				})
			)
		);
	}

	changeConsentDecision(privacyPolicyUrl :string, showProVersionOption :boolean) :Promise<ConsentResult> {
		if (!window['Consent']) {
			console.warn('Consent plugin not present (changeConsentDecision)');
			return Promise.reject('Consent plugin not present');
		}

		return new Promise((resolve, reject) =>
			window['Consent'].changeConsentDecision(privacyPolicyUrl, showProVersionOption,
				(value: ConsentResult) => ngZone.run(() => resolve(value)),
				(value: string) => ngZone.run(() => reject(value))
			)
		);
	}

	isRequestLocationInEeaOrUnknown(isDebug :boolean) :Promise<boolean> {
		if (!window['Consent']) {
			console.warn('Consent plugin not present (isRequestLocationInEeaOrUnknown)');
			return Promise.reject('Consent plugin not present');
		}

		return new Promise((resolve, reject) =>
			window['Consent'].isRequestLocationInEeaOrUnknown(isDebug,
				(value: boolean) => ngZone.run(() => resolve(value)),
				(value: string) => ngZone.run(() => reject(value))
			)
		);
	}

}