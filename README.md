# cordova-plugin-google-consent

This plugin is an ionic cordova wrapper for the google consent sdk.
Google consent sdk is used for asking users in the European Economic Area (EEA) for permission to display personalized ads.

## Installation

```cmd
ionic cordova plugin add cordova-plugin-google-consent
```

## Ionic Include
Include the plugin in your app.module.ts

```typescript
...

import {Consent} from '../../plugins/cordova-plugin-google-consent/www/consent-typescript-wrapper';

...

@NgModule({
  ...

  providers: [
    ...
    Consent
    ...
  ]
  ...
})
export class AppModule { }
})

```


## Supported Platforms

- Android

## Methods

- consent.verifyConsent
- consent.changeConsentDecision

## consent.verifyConsent

Should be called (according to Google) on every app start. Checks and returns consent of the user. If the user has not made any consent decision yet, it asks the users for consent.
If the User is not in the EEA, no dialog is presented to the user. Only users in EEA can make a decision.

### Params
```typescript
verifyConsent(publisherIds :Array<string>, privacyPolicyUrl :string, showProVersionOption :boolean, isDebug :boolean) :Promise<ConsentResult>
```

- publisherIds: Array of all your AdMob ID's used in the app. (see https://support.google.com/admob/answer/2784578)
- privacyPolicyUrl: URL to your GDPR conform privacy policy.
- showProVersionOption: If set to true, on the dialog is shown an option to the user, where he can choose to buy an ad-free pro version.
- isDebug: If set to true, the device acts like it is in the EEA, even if it is not.


### Result
```typescript
interface ConsentResult {
	consent: "PERSONALIZED" | "NON_PERSONALIZED" | "UNKNOWN" | null; // is UNKNOWN in case of the user has chosen to buy the pro option
	isAdFree: boolean; // user has chosen the "buy the pro version" option on the dialog
	hasShownDialog: boolean; // if false, user already made a decision earlier and there was no need to show the dialog
	isNotInEea: boolean; // if true, user is not in EEA and can make no decision. No dialog has been shown to the user if this is the case. Ignore other results if this is the case, you can show personalized ads without asking.
}
```


### Example
```typescript
this.consent.verifyConsent(["pub-123xxxxxxx"], "https://www.mycoolapp.com/privacy",	true, false)
			.then((result) => {
				console.log(result);
			})
			.catch((error) => {
				console.log(error);
			});
```

## consent.changeConsentDecision

Method to change the decision already made. This option should be offered to the user in the settings, that he can change his decision at any time.
This method has no check if a user is really in the EEA or not, the dialog is shown to every user. Use method verifyConsent first to determine if a user is in EEA or not.

### Params
```typescript
changeConsentDecision(privacyPolicyUrl :string, showProVersionOption :boolean) :Promise<ConsentResult>
```

- privacyPolicyUrl: URL to your GDPR conform privacy policy.
- showProVersionOption: If set to true, on the dialog is shown an option to the user, where he can choose to buy an ad-free pro version.


### Result
```typescript
interface ConsentResult {
	consent: "PERSONALIZED" | "NON_PERSONALIZED" | "UNKNOWN" | null; // is UNKNOWN in case of the user has chosen to buy the pro option
	isAdFree: boolean; // user has chosen the to buy the pro option on the dialog
	hasShownDialog: boolean; // in this case is always true
	isNotInEea: boolean; // in this case is always false, because there is no check if the user is really in EEA or not
}
```


### Example
```typescript
this.consent.changeConsentDecision("https://www.mycoolapp.com/privacy", true)
			.then((result) => {
				console.log(result);
			})
			.catch((error) => {
				console.log(error);
			});
```