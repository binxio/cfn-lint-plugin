curl -i --header "Authorization: Bearer perm:bXZhbmhvbHN0ZWlqbg==.OTItMTkwNQ==.WugdpaU7x0iEH9ogLOywmG8UCmgxgN" \
	-F xmlId=io.binx.cfnlint.plugin \
	-F file=@build/distributions/cfn-lint-plugin-0.1.9.zip \
	-F channel=eap \
	https://plugins.jetbrains.com/plugin/uploadPlugin
