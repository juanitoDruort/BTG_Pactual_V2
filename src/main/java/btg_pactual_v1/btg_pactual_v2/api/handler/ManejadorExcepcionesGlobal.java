package btg_pactual_v1.btg_pactual_v2.api.handler;

import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionAccesoDenegado;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionConflicto;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionCredencialesInvalidas;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionCuentaBloqueada;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionTokenInvalido;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ManejadorExcepcionesGlobal {

    @ExceptionHandler(ExcepcionDominio.class)
    public ResponseEntity<Map<String, String>> manejarExcepcionDominio(ExcepcionDominio ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ExcepcionConflicto.class)
    public ResponseEntity<Map<String, String>> manejarExcepcionConflicto(ExcepcionConflicto ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errores", errores));
    }

    @ExceptionHandler(ExcepcionCredencialesInvalidas.class)
    public ResponseEntity<Map<String, String>> manejarCredencialesInvalidas(ExcepcionCredencialesInvalidas ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ExcepcionCuentaBloqueada.class)
    public ResponseEntity<Map<String, String>> manejarCuentaBloqueada(ExcepcionCuentaBloqueada ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ExcepcionTokenInvalido.class)
    public ResponseEntity<Map<String, String>> manejarTokenInvalido(ExcepcionTokenInvalido ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> manejarAccesoDenegadoSpringSecurity(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "No tiene permisos para realizar esta operación"));
    }

    @ExceptionHandler(ExcepcionAccesoDenegado.class)
    public ResponseEntity<Map<String, String>> manejarAccesoDenegado(ExcepcionAccesoDenegado ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }
}
