

## Copy elision
### RVO/NRVO
Some simple text
```c++
A f() {
    return {5};
}

A g() {
    A a(5);
    return a;
}
```