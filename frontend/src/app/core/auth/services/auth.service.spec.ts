import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { AuthenticationService } from '../../../generated/api/authentication.service';
import { LoginRequest } from '../../../generated/model/loginRequest';
import { RegisterRequest } from '../../../generated/model/registerRequest';
import { AuthResponse } from '../../../generated/model/authResponse';

describe('AuthService', () => {
  let service: AuthService;
  let authApiSpy: jasmine.SpyObj<AuthenticationService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let localStorageSpy: jasmine.SpyObj<Storage>;

  const mockAuthResponse: AuthResponse = {
    accessToken: 'mock-access-token',
    refreshToken: 'mock-refresh-token',
    user: {
      id: 'user123',
      name: 'Test User',
      email: 'test@example.com',
      role: 'USER',
    },
  };

  beforeEach(() => {
    const authApiSpyObj = jasmine.createSpyObj('AuthenticationService', ['login', 'register']);
    const routerSpyObj = jasmine.createSpyObj('Router', ['navigate']);

    // Mock localStorage
    let store: { [key: string]: string } = {};
    const mockLocalStorage = {
      getItem: (key: string): string | null => {
        return key in store ? store[key] : null;
      },
      setItem: (key: string, value: string) => {
        store[key] = `${value}`;
      },
      removeItem: (key: string) => {
        delete store[key];
      },
      clear: () => {
        store = {};
      },
    };

    spyOn(localStorage, 'getItem').and.callFake(mockLocalStorage.getItem);
    spyOn(localStorage, 'setItem').and.callFake(mockLocalStorage.setItem);
    spyOn(localStorage, 'removeItem').and.callFake(mockLocalStorage.removeItem);
    spyOn(localStorage, 'clear').and.callFake(mockLocalStorage.clear);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: AuthenticationService, useValue: authApiSpyObj },
        { provide: Router, useValue: routerSpyObj },
      ],
    });

    service = TestBed.inject(AuthService);
    authApiSpy = TestBed.inject(AuthenticationService) as jasmine.SpyObj<AuthenticationService>;
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    // Clear localStorage before each test
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    const loginRequest: LoginRequest = {
      email: 'test@example.com',
      password: 'password123',
    };

    it('should login successfully and store tokens', (done) => {
      authApiSpy.login.and.returnValue(of(mockAuthResponse));

      service.login(loginRequest).subscribe({
        next: (response) => {
          expect(response).toEqual(mockAuthResponse);
          expect(localStorage.setItem).toHaveBeenCalledWith('access_token', 'mock-access-token');
          expect(localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'mock-refresh-token');
          expect(service.currentUser()).toEqual(mockAuthResponse.user);
          expect(service.isAuthenticated()).toBe(true);
          done();
        },
      });
    });

    it('should handle login errors', (done) => {
      authApiSpy.login.and.returnValue(throwError(() => new Error('Invalid credentials')));

      service.login(loginRequest).subscribe({
        error: (err) => {
          expect(err.message).toBe('Invalid credentials');
          expect(service.isAuthenticated()).toBe(false);
          expect(service.currentUser()).toBeNull();
          done();
        },
      });
    });
  });

  describe('register', () => {
    const registerRequest: RegisterRequest = {
      email: 'newuser@example.com',
      password: 'password123',
      name: 'New User',
    };

    it('should register successfully and store tokens', (done) => {
      authApiSpy.register.and.returnValue(of(mockAuthResponse));

      service.register(registerRequest).subscribe({
        next: (response) => {
          expect(response).toEqual(mockAuthResponse);
          expect(localStorage.setItem).toHaveBeenCalledWith('access_token', 'mock-access-token');
          expect(localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'mock-refresh-token');
          expect(service.currentUser()).toEqual(mockAuthResponse.user);
          expect(service.isAuthenticated()).toBe(true);
          done();
        },
      });
    });

    it('should handle registration errors', (done) => {
      authApiSpy.register.and.returnValue(throwError(() => new Error('Email already exists')));

      service.register(registerRequest).subscribe({
        error: (err) => {
          expect(err.message).toBe('Email already exists');
          expect(service.isAuthenticated()).toBe(false);
          done();
        },
      });
    });
  });

  describe('logout', () => {
    beforeEach(() => {
      // Setup authenticated state
      localStorage.setItem('access_token', 'mock-access-token');
      localStorage.setItem('refresh_token', 'mock-refresh-token');
      service.currentUser.set(mockAuthResponse.user!);
    });

    it('should clear tokens and navigate to login', () => {
      service.logout();

      expect(localStorage.removeItem).toHaveBeenCalledWith('access_token');
      expect(localStorage.removeItem).toHaveBeenCalledWith('refresh_token');
      expect(service.currentUser()).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
    });
  });

  describe('getToken', () => {
    it('should return access token from localStorage', () => {
      localStorage.setItem('access_token', 'test-token');
      expect(service.getToken()).toBe('test-token');
    });

    it('should return null if no token exists', () => {
      expect(service.getToken()).toBeNull();
    });
  });

  describe('isAuthenticated', () => {
    it('should return true if user is logged in', () => {
      service.currentUser.set(mockAuthResponse.user!);
      expect(service.isAuthenticated()).toBe(true);
    });

    it('should return false if user is not logged in', () => {
      service.currentUser.set(null);
      expect(service.isAuthenticated()).toBe(false);
    });
  });

  describe('hasRole', () => {
    it('should return true if user has the specified role', () => {
      service.currentUser.set({ ...mockAuthResponse.user!, role: 'PREMIUM' });
      expect(service.hasRole('PREMIUM')).toBe(true);
    });

    it('should return false if user does not have the specified role', () => {
      service.currentUser.set({ ...mockAuthResponse.user!, role: 'USER' });
      expect(service.hasRole('PREMIUM')).toBe(false);
    });

    it('should return false if user is not logged in', () => {
      service.currentUser.set(null);
      expect(service.hasRole('USER')).toBe(false);
    });
  });
});
