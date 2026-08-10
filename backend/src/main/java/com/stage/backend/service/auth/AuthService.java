package com.stage.backend.service.auth;

import com.stage.backend.dto.login.LoginResponse;
import com.stage.backend.dto.utilisateur.CompleteAccountRequest;
import com.stage.backend.dto.utilisateur.SetupAccountInfoResponse;

public interface AuthService {

    LoginResponse login(String email, String password);

    LoginResponse completeAccount(CompleteAccountRequest request);

    SetupAccountInfoResponse getSetupAccountInfo(String token);
}
