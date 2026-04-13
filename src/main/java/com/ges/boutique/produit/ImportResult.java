package com.ges.boutique.produit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
    private int success;
    private int failed;
    private int total;
    private List<String> errors;
    private List<String> details;

    public int getSuccess() { return success; }
    public void setSuccess(int success) { this.success = success; }

    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }
}