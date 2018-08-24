import AdSupport

@objc(Consent)
class Consent : CDVPlugin {
    
    override func pluginInitialize() {
        super.pluginInitialize()
    }
    
    @objc(verifyConsent:)
    func verifyConsent(command: CDVInvokedUrlCommand) {
        let consent = PACConsentInformation.sharedInstance
        
        let publisherIds = command.arguments[0] as? [String]
        if publisherIds == nil {
            self.endError(error: "missing publisher ids", command: command)
            return
        }
        
        let privacyUrlString = command.arguments[1] as? String
        if privacyUrlString == nil {
            //TODO: Error
            self.endError(error: "missing privacy policy url", command: command)
            return
        }
        let showProVersionOption = command.arguments[2] as? Bool ?? false
        let isDebug = command.arguments[3] as? Bool ?? false
        
        print("showProVersionOption: \(showProVersionOption)")
        print("ISDEBUG: \(isDebug)")
        
        var jsonResult = ["hasShownDialog" : false, "isAdFree" : false, "isNotInEea" : false] as [AnyHashable : Any]
        
        
        if isDebug {
            let deviceId = ASIdentifierManager.shared().advertisingIdentifier.uuidString
            print("DEBUG, adding identifier \(deviceId) to debug ids")
            
            PACConsentInformation.sharedInstance.debugIdentifiers = [deviceId]
            PACConsentInformation.sharedInstance.debugGeography = PACDebugGeography.EEA
            //PACConsentInformation.sharedInstance.reset()
        }
        
        consent.requestConsentInfoUpdate(
            forPublisherIdentifiers: publisherIds!)
        {(_ error: Error?) -> Void in
            
            if let error = error {
                print("Error requesting consent: \(error.localizedDescription)")
                self.endError(error: error.localizedDescription, command: command)
            } else {
                if consent.consentStatus == PACConsentStatus.unknown {
                    let isInEeaOrUnknown = PACConsentInformation.sharedInstance.isRequestLocationInEEAOrUnknown
                    
                    //Don't show form to users outside EEA (In fact we can't, the consent SDK throws error if we try...
                    if !isInEeaOrUnknown {
                        print("User is not in EEA, allow personalized...")
                        jsonResult["isNotInEea"] = true
                        jsonResult["consent"] = "UNKNOWN"
                        self.endSuccess(jsonResult: jsonResult, command: command)
                    } else {
                        self.askForConsent(command: command, privacyUrlString: privacyUrlString!, showProVersionOption: showProVersionOption)
                    }
                } else if consent.consentStatus == PACConsentStatus.personalized {
                    print("USER CHOSE PERSONALIZED")
                    jsonResult["consent"] = "PERSONALIZED"
                    self.endSuccess(jsonResult: jsonResult, command: command)
                } else if consent.consentStatus == PACConsentStatus.nonPersonalized {
                    print("USER CHOSE NON_PERSONALIZED")
                    jsonResult["consent"] = "NON_PERSONALIZED"
                    self.endSuccess(jsonResult: jsonResult, command: command)
                }
            }
        }
    }
    
    func endError(error: String, command: CDVInvokedUrlCommand) {
        self.commandDelegate!.send(CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs:error), callbackId: command.callbackId)
    }
    
    func endSuccess(jsonResult : [AnyHashable : Any], command: CDVInvokedUrlCommand) {
        self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_OK, messageAs: jsonResult), callbackId: command.callbackId)
    }
    
    func askForConsent(command: CDVInvokedUrlCommand, privacyUrlString : String, showProVersionOption : Bool) {
        print("WE DONT HAVE CONSENT YET, REQUESTING FORM")
        guard let privacyUrl = URL(string: privacyUrlString),
            let form = PACConsentForm(applicationPrivacyPolicyURL: privacyUrl) else {
                print("incorrect privacy URL.")
                self.endError(error: "malformed privacy policy url", command: command)
                return
        }
        var jsonResult = ["hasShownDialog" : true, "isAdFree" : false, "isNotInEea" : !PACConsentInformation.sharedInstance.isRequestLocationInEEAOrUnknown] as [AnyHashable : Any]
        
        form.shouldOfferPersonalizedAds = true
        form.shouldOfferNonPersonalizedAds = true
        form.shouldOfferAdFree = showProVersionOption
        
        form.load {(_ error: Error?) -> Void in
            print("Load complete.")
            if let error = error {
                // Handle error.
                print("Error loading form: \(error.localizedDescription)")
                self.endError(error: error.localizedDescription, command: command)
            } else {
                form.present(from: self.viewController) { (error, userPrefersAdFree) in
                    let status = PACConsentInformation.sharedInstance.consentStatus
                    if let error = error {
                        // Handle error.
                        print("ERROR: \(error.localizedDescription)")
                        self.endError(error: error.localizedDescription, command: command)
                    } else if userPrefersAdFree {
                        jsonResult["isAdFree"] = true
                        jsonResult["consent"] = "UNKNOWN";
                        self.endSuccess(jsonResult: jsonResult, command: command)
                        print("USER WANTS ADFREE")
                    } else if status == PACConsentStatus.personalized {
                        print("USER CHOSE PERSONALIZED")
                        jsonResult["consent"] = "PERSONALIZED"
                        self.endSuccess(jsonResult: jsonResult, command: command)
                    } else if status == PACConsentStatus.nonPersonalized {
                        print("USER CHOSE NON_PERSONALIZED")
                        jsonResult["consent"] = "NON_PERSONALIZED"
                        self.endSuccess(jsonResult: jsonResult, command: command)
                    }
                }
                // Load successful.
            }
        }
    }
    
    @objc(changeConsentDecision:)
    func changeConsentDecision(command: CDVInvokedUrlCommand) {
        
        let privacyUrlString = command.arguments[0] as? String
        if privacyUrlString == nil {
            //TODO: Error
            self.endError(error: "missing privacy policy url", command: command)
            return
        }
        let showProVersionOption = command.arguments[1] as? Bool ?? false
        
        self.askForConsent(command: command, privacyUrlString: privacyUrlString!, showProVersionOption: showProVersionOption)
    }
}
