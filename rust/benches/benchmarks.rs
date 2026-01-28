use criterion::{black_box, criterion_group, criterion_main, Criterion};
use uuidv7::{
    from_compact_string, generate, generate_compact_string, generate_monotonic,
    reset_monotonic_state, to_compact_string,
};

fn bench_generate(c: &mut Criterion) {
    c.bench_function("generate", |b| b.iter(|| black_box(generate())));
}

fn bench_generate_monotonic(c: &mut Criterion) {
    reset_monotonic_state();
    c.bench_function("generate_monotonic", |b| {
        b.iter(|| black_box(generate_monotonic()))
    });
}

fn bench_to_compact_string(c: &mut Criterion) {
    let uuid = generate();
    c.bench_function("to_compact_string", |b| {
        b.iter(|| black_box(to_compact_string(&uuid)))
    });
}

fn bench_from_compact_string(c: &mut Criterion) {
    let uuid = generate();
    let compact = to_compact_string(&uuid);
    c.bench_function("from_compact_string", |b| {
        b.iter(|| black_box(from_compact_string(&compact)))
    });
}

fn bench_generate_compact_string(c: &mut Criterion) {
    c.bench_function("generate_compact_string", |b| {
        b.iter(|| black_box(generate_compact_string()))
    });
}

fn bench_uuid_to_string(c: &mut Criterion) {
    let uuid = generate();
    c.bench_function("uuid_to_string", |b| b.iter(|| black_box(uuid.to_string())));
}

criterion_group!(
    benches,
    bench_generate,
    bench_generate_monotonic,
    bench_to_compact_string,
    bench_from_compact_string,
    bench_generate_compact_string,
    bench_uuid_to_string,
);
criterion_main!(benches);
