import { Component } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [MatToolbarModule, MatIconModule, MatButtonModule, MatDividerModule],
  template: `
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-section">
          <div class="brand">
            <mat-icon>event</mat-icon>
            <span class="brand-name">GathorApp</span>
          </div>
          <p class="tagline">
            Piattaforma per organizzare e partecipare a eventi ed uscite sociali
          </p>
        </div>

        <div class="footer-section">
          <h3>Link Utili</h3>
          <div class="links">
            <a href="#">Chi Siamo</a>
            <a href="#">Come Funziona</a>
            <a href="#">FAQ</a>
          </div>
        </div>

        <div class="footer-section">
          <h3>Legale</h3>
          <div class="links">
            <a href="#">Privacy Policy</a>
            <a href="#">Termini di Servizio</a>
            <a href="#">Cookie Policy</a>
          </div>
        </div>

        <div class="footer-section">
          <h3>Contatti</h3>
          <div class="links">
            <a href="mailto:info@gathorapp.com">
              <mat-icon>email</mat-icon>
              <span>info@gathorapp.com</span>
            </a>
            <a href="#">
              <mat-icon>language</mat-icon>
              <span>gathorapp.com</span>
            </a>
          </div>
        </div>
      </div>

      <mat-divider></mat-divider>

      <div class="footer-bottom">
        <p class="copyright">
          © {{ currentYear }} GathorApp - Progetto Universitario di Ingegneria Informatica
        </p>
        <div class="social">
          <button mat-icon-button>
            <mat-icon>code</mat-icon>
          </button>
        </div>
      </div>
    </footer>
  `,
  styles: [
    `
      .footer {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        padding: 48px 24px 24px;
        margin-top: auto;
      }

      .footer-content {
        max-width: 1400px;
        margin: 0 auto 32px;
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 32px;
      }

      .footer-section h3 {
        margin: 0 0 16px 0;
        font-size: 18px;
        font-weight: 600;
      }

      .brand {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 12px;

        mat-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;
        }

        .brand-name {
          font-size: 24px;
          font-weight: 600;
        }
      }

      .tagline {
        margin: 0;
        opacity: 0.9;
        font-size: 14px;
        line-height: 1.5;
      }

      .links {
        display: flex;
        flex-direction: column;
        gap: 8px;

        a {
          color: white;
          text-decoration: none;
          opacity: 0.9;
          font-size: 14px;
          display: flex;
          align-items: center;
          gap: 8px;
          transition: opacity 0.2s;

          &:hover {
            opacity: 1;
          }

          mat-icon {
            font-size: 18px;
            width: 18px;
            height: 18px;
          }
        }
      }

      mat-divider {
        background-color: rgba(255, 255, 255, 0.2);
        margin: 24px 0;
      }

      .footer-bottom {
        max-width: 1400px;
        margin: 0 auto;
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 16px;
      }

      .copyright {
        margin: 0;
        font-size: 14px;
        opacity: 0.8;
      }

      .social {
        display: flex;
        gap: 8px;

        button {
          color: white;
        }
      }

      @media (max-width: 768px) {
        .footer {
          padding: 32px 16px 16px;
        }

        .footer-content {
          grid-template-columns: 1fr;
          gap: 24px;
        }

        .footer-bottom {
          flex-direction: column;
          text-align: center;
        }
      }
    `,
  ],
})
export class FooterComponent {
  currentYear = new Date().getFullYear();
}
