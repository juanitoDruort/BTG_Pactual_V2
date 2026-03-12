package btg_pactual_v1.btg_pactual_v2.application.registro.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record RegistroComando(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email no es válido")
        String email,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[0-9]+$", message = "El teléfono solo debe contener números")
        String telefono,

        @NotBlank(message = "El documento de identidad es obligatorio")
        String documentoIdentidad,

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{5,}$",
                message = "La contraseña debe tener mínimo 5 caracteres combinando letras, números y caracteres especiales"
        )
        String contrasena,

        @NotNull(message = "El saldo inicial es obligatorio")
        @Min(value = 500000, message = "El saldo inicial debe ser de al menos COP $500.000")
        BigDecimal saldoInicial
) {}
