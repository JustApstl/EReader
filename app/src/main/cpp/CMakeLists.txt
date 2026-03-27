cmake_minimum_required(VERSION 3.22.1)
project(mobi_native C CXX)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_C_EXTENSIONS OFF)

set(LIBMOBI_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/libmobi/src)

set(LIBMOBI_SOURCES
    ${LIBMOBI_DIR}/buffer.c
    ${LIBMOBI_DIR}/compression.c
    ${LIBMOBI_DIR}/debug.c
    ${LIBMOBI_DIR}/index.c
    ${LIBMOBI_DIR}/memory.c
    ${LIBMOBI_DIR}/meta.c
    ${LIBMOBI_DIR}/parse_rawml.c
    ${LIBMOBI_DIR}/read.c
    ${LIBMOBI_DIR}/structure.c
    ${LIBMOBI_DIR}/util.c
    ${LIBMOBI_DIR}/write.c
    ${LIBMOBI_DIR}/opf.c
    ${LIBMOBI_DIR}/xmlwriter.c
    ${LIBMOBI_DIR}/miniz.c
)

add_library(mobi SHARED ${LIBMOBI_SOURCES})
target_include_directories(mobi PUBLIC ${LIBMOBI_DIR})
target_compile_definitions(mobi PRIVATE
    USE_XMLWRITER
    USE_MINIZ
    _POSIX_C_SOURCE=200112L
)

add_library(mobi_native SHARED mobi_native.cpp)
target_include_directories(mobi_native PRIVATE ${LIBMOBI_DIR})
target_link_libraries(mobi_native mobi log)
