package br.unisinos;

public class Complex {
    public double re;   // the real part
    public double im;   // the imaginary part

    public Complex(double real, double imag) {
        this.re = real;
        this.im = imag;
    }

    public void plus(Complex b) {
        this.re = this.re + b.re;
        this.im = this.im + b.im;
    }

    public void times(Complex c) {
        double real = this.re * c.re - this.im * c.im;
        double imag = this.re * c.im + this.im * c.re;
        this.re = real;
        this.im = imag;
    }

    public double abs() {
        return Math.hypot(re, im);
    }

}
