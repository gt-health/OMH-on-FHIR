import { Component, OnInit } from '@angular/core';
import { OmhonfhirService } from '../omhonfhir/omhonfhir.service';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  patientName;
  patientPhone;
  pageMsg = 'TODO make login page';

  constructor( private omhonfhirService: OmhonfhirService) { }

  ngOnInit() {
  }

  //TODO: How do you determine if you need to login?
  //CALL the <shimmer>/authorizations?username=<shimmerid> URL
  //The response will be JSON with an Array of AccessParameters Objects
  //see the following URL for property details
  //https://github.com/openmhealth/shimmer/blob/ab0ac79d1ce3960b7da2e09ead17a94cc1ee337b/java-shim-sdk/src/main/java/org/openmhealth/shim/AccessParameters.java
  //the endpoint definition can be found here
  //https://github.com/openmhealth/shimmer/blob/master/shim-server/src/main/java/org/openmhealth/shimmer/common/controller/LegacyAuthorizationController.java
  //Basically, if the returned AccessParameters has an entry for fitbit, then
  //the user has authorized access to that account.

  loginWithFitbit(): void{
    this.login("fitbit");
  }

  loginWithGoogleFit(): void{
    this.login("googlefit");
  }

  login( shimKey ): void{
    var patientId = this.omhonfhirService.getPatietId();

    var shimmerAuthUrl =
      environment.omhOnFhirAPIBase +
      environment.omhOnFhirAPIShimmerAuth +
      "?ehrId=" + patientId +
      "&shimkey=" + shimKey;
    console.log("Authorizing with Shimmer " + shimmerAuthUrl);
    window.location.href = shimmerAuthUrl;
    console.log("window url " + window.location.href);
  }
}
