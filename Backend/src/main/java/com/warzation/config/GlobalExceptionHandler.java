package com.warzation.config;

import com.warzation.application.CommandConflictException;
import com.warzation.application.InvalidCommandException;
import com.warzation.application.ResourceNotFoundException;
import com.warzation.application.map.NoRouteException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "A requisição contém dados inválidos", request, fields);
    }

    @ExceptionHandler(InvalidCommandException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCommand(
            InvalidCommandException exception,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_COMMAND", exception.getMessage(), request,
                exception.getFields());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "O corpo da requisição está ausente ou não é um JSON válido", request, Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "A requisição contém dados inválidos", request,
                Map.of(exception.getName(), "valor em formato inválido"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(NoRouteException.class)
    public ResponseEntity<ApiErrorResponse> handleNoRoute(NoRouteException exception, HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "NO_ROUTE", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(CommandConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleCommandConflict(
            CommandConflictException exception,
            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "COMMAND_CONFLICT", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        log.warn("Conflito de integridade em {}: {}", request.getRequestURI(), exception.getMostSpecificCause().getMessage());
        return response(HttpStatus.CONFLICT, "CONFLICT",
                "A operação conflita com o estado atual. Tente novamente.", request, Map.of());
    }

    /**
     * Erros do próprio Spring MVC (rota inexistente, método ou mídia não
     * suportados) mantêm seu status HTTP; qualquer outro erro vira 500 sem
     * expor detalhes ao cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            return response(status, codeFor(status), messageFor(status), request, Map.of());
        }
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocorreu um erro interno. Consulte os logs do servidor.", request, Map.of());
    }

    private static String codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "BAD_REQUEST";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> "HTTP_" + status.value();
        };
    }

    private static String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "Recurso não encontrado";
            case 405 -> "Método HTTP não suportado nesta rota";
            case 415 -> "Tipo de conteúdo não suportado; use application/json";
            default -> "A requisição não pôde ser processada";
        };
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatusCode status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> fields) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(), status.value(), code, message, request.getRequestURI(), fields);
        return ResponseEntity.status(status).body(body);
    }
}
