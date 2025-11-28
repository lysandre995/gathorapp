import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap, catchError, of } from 'rxjs';
import { MapService as MapApiService } from '../../../generated/api/map.service';
import { EventResponse } from '../../../generated/model/eventResponse';
import { OutingResponse } from '../../../generated/model/outingResponse';
import { LocationSuggestion } from '../../../generated/model/locationSuggestion';

/**
 * Service for map and geolocation features
 * Wraps the generated OpenAPI MapService
 */
@Injectable({
  providedIn: 'root',
})
export class MapService {
  private mapApi = inject(MapApiService);

  nearbyEvents = signal<EventResponse[]>([]);
  nearbyOutings = signal<OutingResponse[]>([]);
  locationSuggestions = signal<LocationSuggestion[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  /**
   * Search for nearby events
   * @param latitude User latitude
   * @param longitude User longitude
   * @param radiusKm Search radius in kilometers
   */
  searchNearbyEvents(
    latitude: number,
    longitude: number,
    radiusKm: number
  ): Observable<EventResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.mapApi.getNearbyEvents(latitude, longitude, radiusKm).pipe(
      tap({
        next: (events) => {
          this.nearbyEvents.set(events);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error searching nearby events');
          this.loading.set(false);
          console.error('Error searching nearby events:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error searching nearby events');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Search for nearby outings
   * @param latitude User latitude
   * @param longitude User longitude
   * @param radiusKm Search radius in kilometers
   */
  searchNearbyOutings(
    latitude: number,
    longitude: number,
    radiusKm: number
  ): Observable<OutingResponse[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.mapApi.getNearbyOutings(latitude, longitude, radiusKm).pipe(
      tap({
        next: (outings) => {
          this.nearbyOutings.set(outings);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error searching nearby outings');
          this.loading.set(false);
          console.error('Error searching nearby outings:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error searching nearby outings');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Geocode an address to get coordinates
   * @param address Address to geocode
   */
  geocodeAddress(address: string): Observable<LocationSuggestion[]> {
    this.loading.set(true);
    this.error.set(null);

    return this.mapApi.geocode(address).pipe(
      tap({
        next: (suggestions) => {
          this.locationSuggestions.set(suggestions);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error geocoding address');
          this.loading.set(false);
          console.error('Error geocoding address:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error geocoding address');
        this.loading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Reverse geocode coordinates to get address
   * @param latitude Latitude
   * @param longitude Longitude
   */
  reverseGeocode(latitude: number, longitude: number): Observable<LocationSuggestion> {
    this.loading.set(true);
    this.error.set(null);

    return this.mapApi.reverseGeocode(latitude, longitude).pipe(
      tap({
        next: (suggestion) => {
          this.locationSuggestions.set([suggestion]);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Error reverse geocoding');
          this.loading.set(false);
          console.error('Error reverse geocoding:', err);
        },
      }),
      catchError(() => {
        this.error.set('Error reverse geocoding');
        this.loading.set(false);
        return of({} as LocationSuggestion);
      })
    );
  }

  /**
   * Get user's current position using browser geolocation API
   */
  getCurrentPosition(): Promise<{ latitude: number; longitude: number }> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Geolocation not supported'));
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });
        },
        (error) => {
          reject(error);
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 0,
        }
      );
    });
  }
}
