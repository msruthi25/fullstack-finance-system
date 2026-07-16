import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { Router } from '@angular/router';
import { LoginService } from '../services/login.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  activeTab = signal<'login' | 'signup'>('login');
  showPassword = signal(false);
  showConfirmPassword = signal(false);
  token = "";

  loginForm: FormGroup;
  signupForm: FormGroup;

  constructor(private fb: FormBuilder, private router: Router, private loginService: LoginService) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      rememberMe: [false]
    });

    this.signupForm = this.fb.group(
      {
        firstName: ['', [Validators.required, Validators.minLength(2)]],
        lastName: ['', [Validators.required, Validators.minLength(2)]],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required]
      },
      { validators: this.passwordMatchValidator }
    );
  }

  passwordMatchValidator(group: AbstractControl) {
    const password = group.get('password')?.value;
    const confirm = group.get('confirmPassword')?.value;
    return password === confirm ? null : { passwordMismatch: true };
  }

  setTab(tab: 'login' | 'signup') {
    this.activeTab.set(tab);
  }

  togglePassword() {
    this.showPassword.update(v => !v);
  }

  toggleConfirmPassword() {
    this.showConfirmPassword.update(v => !v);
  }

  onLogin() {
    if (this.loginForm.valid) {
      const login_data = {
        email: this.loginForm.get("email")?.value,
        password: this.loginForm.get("password")?.value
      }
      this.loginService.login(login_data).subscribe({
        next: (res: any) => {
          console.log('Login Success', res);
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          alert("Login Error")
          console.log(err);
        }
      });

    } else {
      this.loginForm.markAllAsTouched();
    }
  }

  onSignup() {
    if (this.signupForm.valid) {
      // TODO: wire up backend register call here
      this.router.navigate(['/dashboard']);
    } else {
      this.signupForm.markAllAsTouched();
    }
  }

  /** Helpers to surface field errors in the template */
  loginError(field: string, error: string) {
    const ctrl = this.loginForm.get(field);
    return ctrl?.hasError(error) && ctrl.touched;
  }

  signupError(field: string, error: string) {
    const ctrl = this.signupForm.get(field);
    return ctrl?.hasError(error) && ctrl.touched;
  }

  get passwordMismatch() {
    return (
      this.signupForm.hasError('passwordMismatch') &&
      this.signupForm.get('confirmPassword')?.touched
    );
  }
}
