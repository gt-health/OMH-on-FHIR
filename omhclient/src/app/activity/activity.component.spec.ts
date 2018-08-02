import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ActivityComponent } from './activity.component';
import { OmhonfhirService } from '../omhonfhir/omhonfhir.service';

const omhonfhirServiceStub = {
  requestDocumentReference(){
    const docref =
      {
        "resourceType":"DocumentReference",
        "status":"current",
        "type":{
          "text":"OMH fitbit data"
        },
        "indexed":"2018-07-31T22:02:11.408+00:00",
        "content":[
         {
           "attachment":{
            "contentType":"application/json",
            "url":"Binary/1d1ddd60-0c42-4ed2-b0e3-8b43876ceb9b",
            "title":"OMH fitbit data",
            "creation":"2018-07-31T22:02:11+00:00"
           }
         }
        ]
      }
    return of( docref );
  }
}

describe('ActivityComponent', () => {
  let component: ActivityComponent;
  let fixture: ComponentFixture<ActivityComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ActivityComponent ],
      providers: [ {provide: OmhonfhirService, useValue: omhonfhirServiceStub}]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ActivityComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  if('should request document reference', () =>{
      component.queryActivity();
      expect(component.activityDocumentRef.resourceType).toBe("DocumentReference");
    });
});
