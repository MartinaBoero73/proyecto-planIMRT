package com.planimrt.forms;

import com.planimrt.model.UserRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserForm {

    @NotEmpty(message = "El nombre de usuario no puede estar vacío")
    private String username;

    @NotEmpty(message = "La contraseña no puede estar vacía")
    @Size(min = 8, max = 32, message = "La contraseña debe tener entre 8 y 32 caracteres")
    private String password;

    @NotEmpty(message = "Debe repetir la contraseña")
    private String passwordConfirm;

    private UserRole role = UserRole.MAINTENANCE_TECHNICIAN;

}
