import { Component, OnInit } from '@angular/core';
import { OmhonfhirService } from '../omhonfhir/omhonfhir.service';

@Component({
  selector: 'app-activity',
  templateUrl: './activity.component.html',
  styleUrls: ['./activity.component.css']
})
export class ActivityComponent implements OnInit {

  startDate: string;
  endDate: string;
  activityJson ;

  constructor( private omhonfhirService: OmhonfhirService) { }

  ngOnInit() {
  }

  queryActivity(): void{
    var patientId = this.omhonfhirService.getPatietId();
    console.log("Querying patient " + patientId + "activity from " + this.startDate + " to " + this.endDate);

    this.omhonfhirService.requestDocumentReference(patientId, this.startDate, this.endDate)
      .subscribe(activityJson => {
        console.log("Processing response");
        this.activityJson = activityJson;
        console.log("Finished processing response");
      });
  }
}
