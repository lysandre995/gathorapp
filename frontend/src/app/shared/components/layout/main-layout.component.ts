import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../navbar/navbar.component';
import { FooterComponent } from '../footer/footer.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent, FooterComponent],
  template: `
    <div class="min-h-screen flex flex-col">
      <app-navbar></app-navbar>

      <main class="flex-1 container mx-auto px-4 py-8">
        <router-outlet></router-outlet>
      </main>

      <app-footer></app-footer>
    </div>
  `,
  styles: [
    `
      .min-h-screen {
        min-height: 100vh;
      }

      .flex {
        display: flex;
      }

      .flex-col {
        flex-direction: column;
      }

      .flex-1 {
        flex: 1;
      }

      .container {
        width: 100%;
        max-width: 1400px;
      }

      .mx-auto {
        margin-left: auto;
        margin-right: auto;
      }

      .px-4 {
        padding-left: 1rem;
        padding-right: 1rem;
      }

      .py-8 {
        padding-top: 2rem;
        padding-bottom: 2rem;
      }

      @media (max-width: 600px) {
        .py-8 {
          padding-top: 1rem;
          padding-bottom: 1rem;
        }
      }
    `,
  ],
})
export class MainLayoutComponent {}
