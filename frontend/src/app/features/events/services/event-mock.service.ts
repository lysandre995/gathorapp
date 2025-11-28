// import { Injectable, signal } from '@angular/core';
// import { Observable, of, delay } from 'rxjs';
// //import { Event } from './event.service';

// @Injectable({
//   providedIn: 'root',
// })
// export class EventMockService {
//   events = signal<Event[]>([]);
//   loading = signal<boolean>(false);
//   error = signal<string | null>(null);

//   private mockEvents: Event[] = [
//     {
//       id: '1',
//       title: 'Aperitivo in Centro',
//       description:
//         'Aperitivo informale per conoscersi e chiacchierare in compagnia. Tutti i benvenuti!',
//       location: 'Piazza Maggiore, Bologna',
//       date: '2025-10-20T18:30:00',
//       maxParticipants: 15,
//       currentParticipants: 8,
//       organizerId: 'user1',
//       organizerName: 'Mario Rossi',
//       status: 'ACTIVE',
//       tags: ['aperitivo', 'sociale', 'centro'],
//       imageUrl: 'https://images.unsplash.com/photo-1566417713940-fe7c737a9ef2?w=400',
//       createdAt: '2025-10-10T10:00:00',
//     },
//     {
//       id: '2',
//       title: 'Escursione sui Colli Bolognesi',
//       description:
//         'Camminata di circa 3 ore sui colli con pranzo al sacco. Livello medio, adatto a tutti gli appassionati di trekking.',
//       location: 'San Luca, Bologna',
//       date: '2025-10-22T09:00:00',
//       maxParticipants: 20,
//       currentParticipants: 12,
//       organizerId: 'user2',
//       organizerName: 'Laura Bianchi',
//       status: 'ACTIVE',
//       tags: ['trekking', 'natura', 'sport'],
//       imageUrl: 'https://images.unsplash.com/photo-1551632811-561732d1e306?w=400',
//       createdAt: '2025-10-11T14:00:00',
//     },
//     {
//       id: '3',
//       title: 'Serata Cinema Indie',
//       description:
//         'Proiezione di film indipendenti seguita da discussione. Portate i vostri snack preferiti!',
//       location: 'Cinema Lumière, Bologna',
//       date: '2025-10-25T20:00:00',
//       maxParticipants: 30,
//       currentParticipants: 18,
//       organizerId: 'user3',
//       organizerName: 'Giuseppe Verdi',
//       status: 'ACTIVE',
//       tags: ['cinema', 'cultura', 'serale'],
//       imageUrl: 'https://images.unsplash.com/photo-1574267432644-f9ae7100c42c?w=400',
//       createdAt: '2025-10-12T16:00:00',
//     },
//     {
//       id: '4',
//       title: 'Corso di Cucina Italiana',
//       description:
//         'Impariamo insieme a preparare la pasta fresca tradizionale. Include cena con i piatti preparati.',
//       location: 'Via Indipendenza 45, Bologna',
//       date: '2025-10-27T19:00:00',
//       maxParticipants: 12,
//       currentParticipants: 10,
//       organizerId: 'user4',
//       organizerName: 'Anna Ferri',
//       status: 'ACTIVE',
//       tags: ['cucina', 'corso', 'food'],
//       imageUrl: 'https://images.unsplash.com/photo-1556910096-6f5e72db6803?w=400',
//       createdAt: '2025-10-13T11:00:00',
//     },
//     {
//       id: '5',
//       title: 'Torneo di Beach Volley',
//       description:
//         'Torneo amichevole 3vs3 sulla spiaggia. Tutti i livelli benvenuti, importante è divertirsi!',
//       location: 'Lido di Classe, Ravenna',
//       date: '2025-10-28T10:00:00',
//       maxParticipants: 24,
//       currentParticipants: 15,
//       organizerId: 'user5',
//       organizerName: 'Marco Neri',
//       status: 'ACTIVE',
//       tags: ['sport', 'beach', 'competitivo'],
//       imageUrl: 'https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?w=400',
//       createdAt: '2025-10-14T09:00:00',
//     },
//     {
//       id: '6',
//       title: 'Visita Guidata MAMbo',
//       description:
//         "Tour guidato al Museo d'Arte Moderna di Bologna con esperto d'arte contemporanea.",
//       location: 'MAMbo, Via Don Minzoni 14, Bologna',
//       date: '2025-10-29T15:00:00',
//       maxParticipants: 25,
//       currentParticipants: 7,
//       organizerId: 'user6',
//       organizerName: 'Chiara Rossi',
//       status: 'ACTIVE',
//       tags: ['arte', 'cultura', 'museo'],
//       imageUrl: 'https://images.unsplash.com/photo-1564399579883-451a5d44ec08?w=400',
//       createdAt: '2025-10-15T12:00:00',
//     },
//     {
//       id: '7',
//       title: 'Jam Session Jazz',
//       description:
//         'Serata jazz aperta a tutti i musicisti. Porta il tuo strumento e vieni a suonare con noi!',
//       location: 'Cantina Bentivoglio, Bologna',
//       date: '2025-10-30T21:00:00',
//       maxParticipants: 40,
//       currentParticipants: 22,
//       organizerId: 'user7',
//       organizerName: 'Paolo Conte',
//       status: 'ACTIVE',
//       tags: ['musica', 'jazz', 'live'],
//       imageUrl: 'https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=400',
//       createdAt: '2025-10-16T18:00:00',
//     },
//     {
//       id: '8',
//       title: 'Corso di Fotografia Urbana',
//       description:
//         'Workshop pratico di fotografia per le strade di Bologna. Porta la tua fotocamera!',
//       location: 'Partenza da Piazza Santo Stefano, Bologna',
//       date: '2025-11-02T14:00:00',
//       maxParticipants: 15,
//       currentParticipants: 11,
//       organizerId: 'user8',
//       organizerName: 'Elena Montanari',
//       status: 'ACTIVE',
//       tags: ['fotografia', 'corso', 'arte'],
//       imageUrl: 'https://images.unsplash.com/photo-1542038784456-1ea8e935640e?w=400',
//       createdAt: '2025-10-17T10:00:00',
//     },
//   ];

//   constructor() {
//     // Inizializza con i dati mock
//     this.events.set(this.mockEvents);
//   }

//   getEvents(): Observable<Event[]> {
//     this.loading.set(true);
//     this.error.set(null);

//     // Simula ritardo di rete
//     return of(this.mockEvents).pipe(
//       delay(800),
//       delay(0) // Reset per evitare problemi
//     );
//   }

//   getEventById(id: string): Observable<Event> {
//     const event = this.mockEvents.find((e) => e.id === id);
//     return of(event!).pipe(delay(500));
//   }

//   getUpcomingEvents(): Observable<Event[]> {
//     this.loading.set(true);
//     this.error.set(null);

//     return new Observable((observer) => {
//       setTimeout(() => {
//         this.events.set(this.mockEvents);
//         this.loading.set(false);
//         observer.next(this.mockEvents);
//         observer.complete();
//       }, 800);
//     });
//   }

//   searchEvents(query: string): Observable<Event[]> {
//     const filtered = this.mockEvents.filter(
//       (event) =>
//         event.title.toLowerCase().includes(query.toLowerCase()) ||
//         event.description.toLowerCase().includes(query.toLowerCase())
//     );

//     this.events.set(filtered);
//     return of(filtered).pipe(delay(300));
//   }
// }
