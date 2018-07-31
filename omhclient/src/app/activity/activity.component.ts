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
  activityJson ;

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
        console.log("Finished processing response");
      });
  }
}
