import { Component, OnInit } from '@angular/core';
import { OmhonfhirService } from '../omhonfhir/omhonfhir.service';
import { switchMap } from 'rxjs/operators';
import { ActivatedRoute, ParamMap } from '@angular/router';
import {DocumentReference} from "../omhonfhir/omhonfhir.service";

@Component({
  selector: 'app-activity',
  templateUrl: './activity.component.html',
  styleUrls: ['./activity.component.css']
})
export class ActivityComponent implements OnInit {

  startDate: string;
  endDate: string;
  shimmerId: string;
  activityDocumentRef: DocumentReference;
  activityJsonString: string;
  activityResourceType: string = "application/json";
  activityDataType: string = "OMH JSON";
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
      .subscribe((documentReference: DocumentReference) => {
        console.log("Processing response");
        this.activityDocumentRef = documentReference;
        this.activityJsonString = JSON.stringify(documentReference);
        //sample response
        //{
        // "resourceType":"DocumentReference",
        // "status":"current",
        // "type":{
        //    "text":"OMH fitbit data"
        // },
        // "indexed":"2018-07-31T22:02:11.408+00:00",
        // "content":[
        //     {
        //       "attachment":{
        //        "contentType":"application/json",
        //        "url":"Binary/1d1ddd60-0c42-4ed2-b0e3-8b43876ceb9b",
        //        "title":"OMH fitbit data",
        //        "creation":"2018-07-31T22:02:11+00:00"
        //       }
        //     }
        // ]
        //}
        //
        //make title
        this.activityResourceType = documentReference.resourceType;//documentReference['resourceType'];
        //make type
        this.activityDataType = documentReference.type.text;//documentReference['type']['text'];
        //make url
        this.activityBinaryUrl = documentReference.content[0].attachment.url;//documentReference['type']['content'][0]['url'];
        console.log("Finished processing response " + this.activityJsonString);
      });
  }

  queryBinary(): void{
    console.log("Querying binary " + this.activityBinaryUrl);
    this.omhonfhirService.requestBinary(this.activityBinaryUrl);
  }
}
