import { Component, OnInit } from '@angular/core';
import { OmhonfhirService } from '../omhonfhir/omhonfhir.service';
import { switchMap } from 'rxjs/operators';
import { ActivatedRoute, ParamMap } from '@angular/router';

@Component({
  selector: 'app-activity',
  templateUrl: './activity.component.html',
  styleUrls: ['./activity.component.css']
})
export class ActivityComponent implements OnInit {

  startDate: string;
  endDate: string;
  shimmerId: string;
  activityJson;
  activityJsonString: string;
  activityResourceType: string;
  activityDataType: string;
  activityBinaryUrl: string;

  constructor( private omhonfhirService: OmhonfhirService,
               private route: ActivatedRoute) { }

  ngOnInit() {
    this.route.queryParamMap.subscribe(params => {
      console.log("Activity Component parsing params: " + params.keys);
      this.shimmerId = params.get("shimmerId");
      console.log("ShimmerId: " + this.shimmerId);
    });
  }

  queryActivity(): void{
    //var patientId = this.omhonfhirService.getPatietId();
    console.log("Querying patient " + this.shimmerId+ "activity from " + this.startDate + " to " + this.endDate);

    this.omhonfhirService.requestDocumentReference(this.shimmerId, this.startDate, this.endDate)
      .subscribe(activityJson => {
        console.log("Processing response");
        this.activityJson = activityJson;
        this.activityJsonString = JSON.stringify(activityJson);
        //sample response
        //{"resourceType":"DocumentReference","status":"current","type":{"text":"OMH fitbit data"},"indexed":"2018-07-31T22:02:11.408+00:00","content":[{"attachment":{"contentType":"application/json","url":"Binary/1d1ddd60-0c42-4ed2-b0e3-8b43876ceb9b","title":"OMH fitbit data","creation":"2018-07-31T22:02:11+00:00"}}]}
        //make title
        this.activityResourceType = activityJson['resourceType'];
        //make type
        this.activityDataType = activityJson['type']['text'];
        //make url
        this.activityBinaryUrl = activityJson['type']['content'][0]['url'];
        console.log("Finished processing response " + this.activityJsonString);
      });
  }

  queryBinary(): void{
    console.log("Querying binary " + this.activityBinaryUrl);
    this.omhonfhirService.requestBinary(this.activityBinaryUrl);
  }
}
