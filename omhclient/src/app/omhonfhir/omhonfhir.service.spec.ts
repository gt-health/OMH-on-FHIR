import { TestBed, inject } from '@angular/core/testing';

import { OmhonfhirService } from './omhonfhir.service';

describe('OmhonfhirService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [OmhonfhirService]
    });
  });

  it('should be created', inject([OmhonfhirService], (service: OmhonfhirService) => {
    expect(service).toBeTruthy();
  }));
});
