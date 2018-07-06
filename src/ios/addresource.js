module.exports = function(ctx) {
    var fs = ctx.requireCordovaModule('fs'),
        path = ctx.requireCordovaModule('path'),
        glob = ctx.requireCordovaModule('glob'),
        deferral = ctx.requireCordovaModule('q').defer();

    var platformRoot = path.join(ctx.opts.projectRoot, 'platforms/ios');

    var sdkFile = path.join(platformRoot, 'Pods/PersonalizedAdConsent/PersonalizedAdConsent/PersonalizedAdConsent/PersonalizedAdConsent.bundle/consentform.html');
    if (!fs.existsSync(sdkFile)) {
        deferral.reject('Failed to find installed Google Consent SDK with consentform.html file');
        return;
    }
    glob.glob('**/Resources/PersonalizedAdConsent.bundle/consentform.html', {cwd: platformRoot}, function(err, files) {
        if (files.length !== 1) {
            deferral.reject('Failed to find consentform.html installed as a resource');
            return;
        }
        var resourceFile = path.join(platformRoot, files[0]);

        var content = fs.readFileSync(resourceFile);
        if (content.indexOf('***PLACEHOLDER FILE***') == -1) {
            console.log('consentform.html already installed, no need to copy again.');
            deferral.resolve();
            return;
        }

        var htmlSource = fs.readFileSync(sdkFile, 'utf-8');
        fs.writeFileSync(resourceFile, htmlSource, 'utf-8');
        console.log('Copied ' + sdkFile + ' to ' + resourceFile);
        deferral.resolve();

    });

    return deferral.promise;
};