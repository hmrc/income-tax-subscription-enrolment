
# Income Tax Subscription Enrolment

This service is used to migrate users from existing Self Assessment (IR-SA) enrolments to the Making Tax Digital (HMRC-MTD-IT)
enrolment.

### Running

Run the following commands to run the service locally

```
sm2 --start ITSA_ENROL_ALL
```

```
sm2 --stop INCOME_TAX_SUBSCRIPTION_ENROLMENT
```

```
sbt run
```

```
curl -XPOST http://localhost:9595/enrolment-store-stub/data -H "content-type: application/json" -d '{
	"groupId": "90ccf333-65d2-4bf2-a008-01dfca702161",
	"affinityGroup": "Organisation",
	"users": [
		{
			"credId": "00000123450",
			"name": "Default User",
			"email": "default@example.com",
			"credentialRole": "Admin",
			"description": "User Description"
		}
	],
	"enrolments": [
		{
			"serviceName": "IR-SA",
			"identifiers": [
				{
					"key": "UTR",
					"value": "1234567890"
				}
			],
			"enrolmentFriendlyName": "IR SA Enrolment",
			"assignedUserCreds": [
				"00000123450"
			],
			"state": "Activated",
			"enrolmentType": "principal",
			"assignedToAll": false
		}
	]
}'
```

```
curl http://localhost:9564/income-tax-subscription-enrolment/enrol --header "Content-Type: application/json" --request POST --data '{"nino":"AB123456C","utr":"1234567890","mtdbsa":"qwer00000000000"}'
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").