import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { OmhonfhirService } from '../omhonfhir/omhonfhir.service';
import { switchMap } from 'rxjs/operators';
import { ActivatedRoute, ParamMap } from '@angular/router';
import {DocumentReference, Binary, OmhActivity, DocRefBundle} from "../omhonfhir/omhonfhir.service";

@Component({
  selector: 'app-activity',
  templateUrl: './activity.component.html',
  styleUrls: ['./activity.component.css']
})
export class ActivityComponent implements OnInit {
  waitingForSearch = false;
  waitingForData = false;
  startDate: string;
  endDate: string;
  shimmerId: string;
  activityDocumentRef: DocumentReference;
  activityResourceType: string = "application/json";
  activityDataType: string = "OMH JSON";
  activityBinaryUrl: string;
  omhActivity: OmhActivity;

  constructor( private omhonfhirService: OmhonfhirService,
               private route: ActivatedRoute) { }

  ngOnInit() {
    this.route.queryParamMap.subscribe(params => {
      console.log("Activity Component parsing params: " + params.keys);
      this.shimmerId = params.get("shimmerId");
      console.log("ShimmerId: " + this.shimmerId);
    });

    //initialize the start date
    var date = new Date();
    var datePipe = new DatePipe('en-US');
    var formattedDate = datePipe.transform(date, 'yyyy-MM-dd');
    this.startDate = formattedDate;
    this.endDate = formattedDate;
    console.log("Set start date: " + this.startDate);
    console.log("Set end date: " + this.endDate);
  }

  queryActivity(): void{
    //var patientId = this.omhonfhirService.getPatietId();
    console.log("Querying patient " + this.shimmerId+ "activity from " + this.startDate + " to " + this.endDate);
    this.waitingForSearch = true;
    this.omhonfhirService.requestDocumentReference(this.shimmerId, this.startDate, this.endDate)
      .subscribe((documentReference: DocRefBundle) => {
        console.log("Processing response");
        //at the moment we are returning a single entry in the response
        var currDocRef = documentReference.entry[0].resource;
        this.activityDocumentRef = currDocRef;
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
        this.activityResourceType = currDocRef.resourceType;//documentReference['resourceType'];
        //make type
        this.activityDataType = currDocRef.type.text;//documentReference['type']['text'];
        //make url
        this.activityBinaryUrl = currDocRef.content[0].attachment.url;//documentReference['type']['content'][0]['url'];
        this.waitingForSearch = false;
      });
  }

  queryBinary(): void{
    console.log("Querying binary " + this.activityBinaryUrl);
    this.waitingForData = true;
    //this.omhonfhirService.requestBinary(this.activityBinaryUrl).subscribe((binary: Binary) => {
    this.omhonfhirService.requestBinaryAsJson(this.activityBinaryUrl).subscribe((omhActivity: OmhActivity) => {
      console.log("Processing Binary response");
      console.log(omhActivity);
      this.omhActivity = omhActivity; //to convert OmhActivity to JSON string use JSON.stringify(omhActivity)
      console.log("processed response");
      this.waitingForData = false;
    });
  }
}
