import { Component, OnInit, OnDestroy, AfterViewInit, inject, signal } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';

// Angular Material
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSliderModule } from '@angular/material/slider';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { MapService } from '../../services/map.service';
import { EventCardComponent } from '../../../events/components/event-card/event-card.component';
import { OutingCardComponent } from '../../../outings/components/outing-card/outing-card.component';

/**
 * Map view component for proximity search of events and outings
 * Uses geolocation and OpenStreetMap Nominatim for geocoding
 */
@Component({
  selector: 'app-map-view',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSliderModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    EventCardComponent,
    OutingCardComponent,
  ],
  template: `
    <div class="map-container">
      <!-- Interactive Map -->
      <div id="map" class="leaflet-map"></div>

      <mat-card class="search-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>map</mat-icon>
            Nearby Search
          </mat-card-title>
        </mat-card-header>

        <mat-card-content>
          <div class="search-controls">
            <!-- Location input -->
            <mat-form-field appearance="outline" class="location-field">
              <mat-label>Your Location</mat-label>
              <input
                matInput
                [(ngModel)]="locationInput"
                placeholder="Address or click 'Use My Location'"
                (keyup.enter)="searchNearby()"
              />
              <mat-icon matPrefix>location_on</mat-icon>
            </mat-form-field>

            <button
              mat-raised-button
              color="primary"
              (click)="useCurrentLocation()"
              [disabled]="mapService.loading()"
            >
              <mat-icon>my_location</mat-icon>
              Use My Location
            </button>

            <!-- Radius slider -->
            <div class="radius-control">
              <label>Search Radius: {{ radiusKm() }} km</label>
              <mat-slider min="1" max="50" step="1" [(ngModel)]="radiusKm" discrete>
                <input matSliderThumb />
              </mat-slider>
            </div>

            <!-- Search button -->
            <button
              mat-raised-button
              color="accent"
              (click)="searchNearby()"
              [disabled]="(!locationInput.trim() && !currentLat() && !currentLon()) || mapService.loading()"
              class="search-button"
            >
              <mat-icon>search</mat-icon>
              Search Nearby
            </button>
          </div>

          @if (currentLat() && currentLon()) {
          <div class="coordinates-display">
            <mat-icon>place</mat-icon>
            <span
              >Current coordinates: {{ currentLat()?.toFixed(4) }}, {{
                currentLon()?.toFixed(4)
              }}</span
            >
          </div>
          }
        </mat-card-content>
      </mat-card>

      <!-- Results -->
      @if (mapService.loading()) {
      <div class="loading-container">
        <mat-spinner></mat-spinner>
        <p>Searching nearby...</p>
      </div>
      }

      @if (mapService.error()) {
      <mat-card class="error-card">
        <mat-icon>error_outline</mat-icon>
        <h3>Error</h3>
        <p>{{ mapService.error() }}</p>
      </mat-card>
      }

      @if (!mapService.loading() && !mapService.error() && searchPerformed()) {
      <mat-card class="results-card">
        <mat-tab-group>
          <!-- Events tab -->
          <mat-tab label="Events ({{ mapService.nearbyEvents().length }})">
            <div class="results-grid">
              @if (mapService.nearbyEvents().length > 0) { @for (event of mapService.nearbyEvents();
              track event.id) {
              <app-event-card [event]="event" />
              } } @else {
              <div class="empty-state">
                <mat-icon>event_busy</mat-icon>
                <p>No events found in this area</p>
              </div>
              }
            </div>
          </mat-tab>

          <!-- Outings tab -->
          <mat-tab label="Outings ({{ mapService.nearbyOutings().length }})">
            <div class="results-grid">
              @if (mapService.nearbyOutings().length > 0) { @for (outing of
              mapService.nearbyOutings(); track outing.id) {
              <app-outing-card [outing]="outing" />
              } } @else {
              <div class="empty-state">
                <mat-icon>explore_off</mat-icon>
                <p>No outings found in this area</p>
              </div>
              }
            </div>
          </mat-tab>
        </mat-tab-group>
      </mat-card>
      }
    </div>
  `,
  styles: [
    `
      .map-container {
        max-width: 1400px;
        margin: 24px auto;
        padding: 24px;
        position: relative;
      }

      .leaflet-map {
        height: 500px;
        width: 100%;
        margin-bottom: 24px;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        z-index: 1;
      }

      .search-card {
        margin-bottom: 24px;

        mat-card-header {
          mat-card-title {
            display: flex;
            align-items: center;
            gap: 12px;
            font-size: 24px;
          }
        }
      }

      .search-controls {
        display: flex;
        flex-direction: column;
        gap: 16px;

        .location-field {
          width: 100%;
        }

        .radius-control {
          display: flex;
          flex-direction: column;
          gap: 8px;

          label {
            font-weight: 500;
            color: #666;
          }

          mat-slider {
            width: 100%;
          }
        }

        .search-button {
          width: 100%;
        }
      }

      .coordinates-display {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-top: 16px;
        padding: 12px;
        background-color: #f5f5f5;
        border-radius: 8px;
        font-size: 14px;
        color: #666;

        mat-icon {
          font-size: 20px;
          width: 20px;
          height: 20px;
          color: #1976d2;
        }
      }

      .loading-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 64px;
        gap: 16px;
      }

      .error-card {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 32px;
        text-align: center;

        mat-icon {
          font-size: 48px;
          width: 48px;
          height: 48px;
          color: #f44336;
          margin-bottom: 16px;
        }

        h3 {
          margin: 0 0 8px 0;
          color: #f44336;
        }
      }

      .results-card {
        mat-tab-group {
          margin-top: 16px;
        }
      }

      .results-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
        gap: 24px;
        padding: 24px;
      }

      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 64px;
        color: #999;
        grid-column: 1 / -1;

        mat-icon {
          font-size: 64px;
          width: 64px;
          height: 64px;
          margin-bottom: 16px;
        }

        p {
          font-size: 16px;
        }
      }

      @media (max-width: 768px) {
        .map-container {
          padding: 16px;
        }

        .results-grid {
          grid-template-columns: 1fr;
        }

        .leaflet-map {
          height: 300px;
        }
      }

      /* Leaflet popup styles */
      :host ::ng-deep .marker-popup {
        h4 {
          margin: 0 0 8px 0;
          font-size: 16px;
          font-weight: 600;
        }

        p {
          margin: 4px 0;
          font-size: 14px;
        }

        strong {
          color: #1976d2;
        }
      }

      /* Different marker colors */
      :host ::ng-deep .event-marker {
        filter: hue-rotate(0deg);
      }

      :host ::ng-deep .outing-marker {
        filter: hue-rotate(220deg);
      }
    `,
  ],
})
export class MapViewComponent implements OnInit, AfterViewInit, OnDestroy {
  mapService = inject(MapService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);

  locationInput = '';
  radiusKm = signal(25); // Increased default radius to 25km
  currentLat = signal<number | null>(null);
  currentLon = signal<number | null>(null);
  searchPerformed = signal(false);

  // Leaflet map
  private map: L.Map | null = null;
  private markers: L.Marker[] = [];
  private defaultMapCenter = signal<{ lat: number; lon: number }>({
    lat: 41.9028, // Rome, Italy (fallback)
    lon: 12.4964
  });

  ngOnInit() {
    // Check for lat/lng/zoom from queryParams (from event detail "View on Map")
    this.route.queryParams.subscribe((params) => {
      const lat = params['lat'];
      const lng = params['lng'];
      const zoom = params['zoom'];

      if (lat && lng) {
        const latitude = parseFloat(lat);
        const longitude = parseFloat(lng);
        const zoomLevel = zoom ? parseInt(zoom, 10) : 15;

        // Set center to event coordinates
        this.defaultMapCenter.set({ lat: latitude, lon: longitude });
        this.currentLat.set(latitude);
        this.currentLon.set(longitude);

        // If map already initialized, fly to location
        if (this.map) {
          this.map.flyTo([latitude, longitude], zoomLevel);
        }
      } else {
        // Try to detect country capital on init
        this.detectCountryCapital();
      }
    });
  }

  ngAfterViewInit() {
    // Init map will be called after country detection
    setTimeout(() => {
      if (!this.map) {
        this.initMap();
      }
    }, 100);
  }

  ngOnDestroy() {
    if (this.map) {
      this.map.remove();
    }
  }

  /**
   * Detect user's country and set map center to capital
   */
  private detectCountryCapital() {
    // Use ip-api.com free geolocation service
    fetch('http://ip-api.com/json/')
      .then(response => response.json())
      .then(data => {
        if (data.status === 'success') {
          const country = data.country;
          const countryCode = data.countryCode;

          // Map of country codes to capital coordinates
          const capitalCoordinates: { [key: string]: { lat: number; lon: number } } = {
            'IT': { lat: 41.9028, lon: 12.4964 },   // Rome
            'US': { lat: 38.9072, lon: -77.0369 },  // Washington D.C.
            'GB': { lat: 51.5074, lon: -0.1278 },   // London
            'FR': { lat: 48.8566, lon: 2.3522 },    // Paris
            'DE': { lat: 52.5200, lon: 13.4050 },   // Berlin
            'ES': { lat: 40.4168, lon: -3.7038 },   // Madrid
            'PT': { lat: 38.7223, lon: -9.1393 },   // Lisbon
            'NL': { lat: 52.3676, lon: 4.9041 },    // Amsterdam
            'BE': { lat: 50.8503, lon: 4.3517 },    // Brussels
            'CH': { lat: 46.9480, lon: 7.4474 },    // Bern
            'AT': { lat: 48.2082, lon: 16.3738 },   // Vienna
            'PL': { lat: 52.2297, lon: 21.0122 },   // Warsaw
            'SE': { lat: 59.3293, lon: 18.0686 },   // Stockholm
            'NO': { lat: 59.9139, lon: 10.7522 },   // Oslo
            'DK': { lat: 55.6761, lon: 12.5683 },   // Copenhagen
            'FI': { lat: 60.1699, lon: 24.9384 },   // Helsinki
            'GR': { lat: 37.9838, lon: 23.7275 },   // Athens
            'JP': { lat: 35.6762, lon: 139.6503 },  // Tokyo
            'CN': { lat: 39.9042, lon: 116.4074 },  // Beijing
            'IN': { lat: 28.6139, lon: 77.2090 },   // New Delhi
            'BR': { lat: -15.8267, lon: -47.9218 }, // Brasília
            'CA': { lat: 45.4215, lon: -75.6972 },  // Ottawa
            'MX': { lat: 19.4326, lon: -99.1332 },  // Mexico City
            'AU': { lat: -35.2809, lon: 149.1300 }, // Canberra
          };

          const capital = capitalCoordinates[countryCode];
          if (capital) {
            this.defaultMapCenter.set(capital);
            console.log(`Map centered on ${country} capital:`, capital);
          } else {
            // Fallback to user's current lat/lon from IP
            if (data.lat && data.lon) {
              this.defaultMapCenter.set({ lat: data.lat, lon: data.lon });
              console.log(`Map centered on user location in ${country}:`, { lat: data.lat, lon: data.lon });
            }
          }

          // Initialize map after detection
          if (!this.map) {
            this.initMap();
          }
        }
      })
      .catch(error => {
        console.warn('Could not detect country, using default location (Rome)', error);
        // Initialize map with default on error
        if (!this.map) {
          this.initMap();
        }
      });
  }

  /**
   * Initialize Leaflet map
   */
  private initMap() {
    const center = this.defaultMapCenter();
    const defaultLat = center.lat;
    const defaultLon = center.lon;

    // Fix Leaflet icon issue with Angular
    const iconRetinaUrl = 'assets/leaflet/marker-icon-2x.png';
    const iconUrl = 'assets/leaflet/marker-icon.png';
    const shadowUrl = 'assets/leaflet/marker-shadow.png';
    const iconDefault = L.icon({
      iconRetinaUrl,
      iconUrl,
      shadowUrl,
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      tooltipAnchor: [16, -28],
      shadowSize: [41, 41]
    });
    L.Marker.prototype.options.icon = iconDefault;

    // Create map
    this.map = L.map('map').setView([defaultLat, defaultLon], 13);

    // Add OpenStreetMap tiles
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(this.map);
  }

  /**
   * Clear all markers from the map
   */
  private clearMarkers() {
    this.markers.forEach(marker => marker.remove());
    this.markers = [];
  }

  /**
   * Add markers for events and outings
   */
  private updateMapMarkers() {
    if (!this.map) return;

    console.log('updateMapMarkers called');
    console.log('Nearby events:', this.mapService.nearbyEvents());
    console.log('Nearby outings:', this.mapService.nearbyOutings());

    this.clearMarkers();

    // Add event markers (red)
    const eventIcon = L.icon({
      iconUrl: 'assets/leaflet/marker-icon.png',
      iconRetinaUrl: 'assets/leaflet/marker-icon-2x.png',
      shadowUrl: 'assets/leaflet/marker-shadow.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41],
      className: 'event-marker'
    });

    this.mapService.nearbyEvents().forEach(event => {
      if (event.latitude && event.longitude && this.map) {
        const marker = L.marker([event.latitude, event.longitude], { icon: eventIcon })
          .addTo(this.map)
          .bindPopup(`
            <div class="marker-popup">
              <h4>${event.title || 'Event'}</h4>
              <p><strong>Event</strong></p>
              <p>${event.location || 'No location'}</p>
              <p>${event.eventDate ? new Date(event.eventDate).toLocaleDateString() : ''}</p>
            </div>
          `);
        marker.on('click', () => {
          if (event.id) {
            this.onViewEventDetails(event.id);
          }
        });
        this.markers.push(marker);
      }
    });

    // Add outing markers (blue)
    const outingIcon = L.icon({
      iconUrl: 'assets/leaflet/marker-icon.png',
      iconRetinaUrl: 'assets/leaflet/marker-icon-2x.png',
      shadowUrl: 'assets/leaflet/marker-shadow.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41],
      className: 'outing-marker'
    });

    this.mapService.nearbyOutings().forEach(outing => {
      if (outing.latitude && outing.longitude && this.map) {
        const marker = L.marker([outing.latitude, outing.longitude], { icon: outingIcon })
          .addTo(this.map)
          .bindPopup(`
            <div class="marker-popup">
              <h4>${outing.title || 'Outing'}</h4>
              <p><strong>Outing</strong></p>
              <p>${outing.location || 'No location'}</p>
              <p>Max Participants: ${outing.maxParticipants || 0}</p>
            </div>
          `);
        marker.on('click', () => {
          if (outing.id) {
            this.onViewOutingDetails(outing.id);
          }
        });
        this.markers.push(marker);
      }
    });

    // Fit map to markers if any exist
    if (this.markers.length > 0) {
      const group = L.featureGroup(this.markers);
      this.map.fitBounds(group.getBounds().pad(0.1));
    }
  }

  /**
   * Use browser geolocation to get current position
   */
  useCurrentLocation() {
    this.mapService
      .getCurrentPosition()
      .then((position) => {
        this.currentLat.set(position.latitude);
        this.currentLon.set(position.longitude);
        this.locationInput = `${position.latitude.toFixed(4)}, ${position.longitude.toFixed(4)}`;
        this.snackBar.open('Location detected!', 'Close', { duration: 3000 });

        // Optionally reverse geocode to get address
        this.mapService.reverseGeocode(position.latitude, position.longitude).subscribe({
          next: (suggestion) => {
            if (suggestion.display_name) {
              this.locationInput = suggestion.display_name;
            }
          },
        });
      })
      .catch((error) => {
        console.error('Geolocation error:', error);
        this.snackBar.open('Could not detect location. Please enter manually.', 'Close', {
          duration: 5000,
        });
      });
  }

  /**
   * Search for nearby events and outings
   */
  searchNearby() {
    // If user entered an address, geocode it first
    if (this.locationInput.trim()) {
      this.mapService.geocodeAddress(this.locationInput).subscribe({
        next: (suggestions) => {
          if (suggestions.length > 0) {
            const firstSuggestion = suggestions[0];
            this.currentLat.set(firstSuggestion.latitudeAsDouble || 0);
            this.currentLon.set(firstSuggestion.longitudeAsDouble || 0);
            // Update location input with the found address
            if (firstSuggestion.display_name) {
              this.locationInput = firstSuggestion.display_name;
            }
            this.snackBar.open('Location found!', 'Close', { duration: 2000 });
            this.performSearch();
          } else {
            this.snackBar.open('Could not find location. Try another address.', 'Close', {
              duration: 5000,
            });
          }
        },
        error: () => {
          this.snackBar.open('Error geocoding address', 'Close', { duration: 3000 });
        },
      });
      return;
    }

    // Otherwise, use current coordinates if available
    if (this.currentLat() && this.currentLon()) {
      this.performSearch();
    } else {
      this.snackBar.open('Please enter a location or use your current location', 'Close', {
        duration: 3000,
      });
    }
  }

  /**
   * Perform the actual proximity search
   */
  private performSearch() {
    const lat = this.currentLat()!;
    const lon = this.currentLon()!;
    const radius = this.radiusKm();

    console.log('Performing search with:', { lat, lon, radius });
    console.log('API call will be: GET /api/map/nearby/events?latitude=' + lat + '&longitude=' + lon + '&radiusKm=' + radius);

    // Center map on search location
    if (this.map) {
      this.map.setView([lat, lon], 13);
    }

    // Search for both events and outings
    this.mapService.searchNearbyEvents(lat, lon, radius).subscribe({
      next: (events) => {
        console.log('Events received:', events);
        console.log('Service nearbyEvents signal:', this.mapService.nearbyEvents());
        this.updateMapMarkers();
      },
      error: (err) => {
        console.error('Error fetching events:', err);
      }
    });
    this.mapService.searchNearbyOutings(lat, lon, radius).subscribe({
      next: (outings) => {
        console.log('Outings received:', outings);
        console.log('Service nearbyOutings signal:', this.mapService.nearbyOutings());
        this.updateMapMarkers();
      },
      error: (err) => {
        console.error('Error fetching outings:', err);
      }
    });

    this.searchPerformed.set(true);
  }

  /**
   * Navigate to event details
   */
  onViewEventDetails(eventId: string) {
    this.router.navigate(['/events', eventId]);
  }

  /**
   * Navigate to outing details
   */
  onViewOutingDetails(outingId: string) {
    this.router.navigate(['/outings', outingId]);
  }
}
