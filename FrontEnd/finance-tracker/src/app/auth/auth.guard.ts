import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, GuardResult, MaybeAsync, Router, RouterStateSnapshot } from '@angular/router';
import { map, Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
    //Prevents user from opening protected pages
    constructor(private authService: AuthService, private router: Router) { }


    canActivate(): Observable<boolean> {

        return this.authService.isLoggedIn().pipe(
            map(isLoggedIn => {

                if (!isLoggedIn) {
                    this.router.navigate(['/login']);
                    return false;
                }

                return true;
            })
        );
    }

}