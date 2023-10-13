# Motivation to move from ANTLR to Kaitai:
# https://github.com/antlr/antlr4/issues/828#issuecomment-1249695942

meta:
  id: hprof
  file-extension: hprof
  endian: be
seq:
  - id: header
    type: header
  - id: tags
    type: tag
    repeat: eos
enums:
  basic_type:
    2: object
    4: boolean
    5: char
    6: float
    7: double
    8: byte
    9: short
    10: int
    11: long
  tag_type:
    0x01: string
    0x02: load_class
    0x03: unload_class
    0x04: stack_frame
    0x05: stack_trace
    0x06: alloc_sites
    0x07: heap_summary
    0x0A: start_thread
    0x0B: end_thread
    0x0C: heap_dump
    0x1C: heap_dump_segment
    0x2C: heap_dump_end
    0x0D: cpu_samples
    0x0E: control_settings
  sub_tag_type:
    0xFF: root_unknown
    0x01: root_jni_global
    0x02: root_jni_local
    0x03: root_java_frame
    0x04: root_native_stack
    0x05: root_sticky_class
    0x06: root_thread_block
    0x07: root_monitor_used
    0x08: root_thread_object
    0x20: class_dump
    0x21: instance_dump
    0x22: object_array_dump
    0x23: primitive_array_dump
types:
  uid:
    seq:
      - id: identifier
        type:
          switch-on: _root.header.uid_size.as<s4>
          cases:
            4: u4
            8: u8
  header:
    seq:
      - id: profile_version
        type: strz
        encoding: UTF-8
      - id: uid_size
        type: u4
      - id: gmt_ms
        type: u8
  tag:
    seq:
      - id: record_type
        type: u1
        enum: tag_type
      - id: time
        type: u4
      - id: body_length
        type: u4
      - id: body
        size: body_length
        type:
          switch-on: record_type
          cases:
            tag_type::string: tag_string
            tag_type::load_class: tag_load_class
            tag_type::unload_class: tag_unload_class
            tag_type::stack_frame: tag_stack_frame
            tag_type::stack_trace: tag_stack_trace
            tag_type::alloc_sites: tag_alloc_sites
            tag_type::heap_summary: tag_heap_summary
            tag_type::start_thread: tag_start_thread
            tag_type::end_thread: tag_end_thread
            tag_type::heap_dump: tag_heap_dump
            tag_type::heap_dump_segment: tag_heap_dump_segment
            tag_type::heap_dump_end: tag_heap_dump_end
            tag_type::cpu_samples: tag_cpu_samples
            tag_type::control_settings: tag_control_settings
  tag_string:
    seq:
      - id: string_id
        type: uid
      - id: string
        type: str
        encoding: UTF-8
        size: _parent.body_length - _root.header.uid_size
  tag_load_class:
    seq:
      - id: class_serial_num
        type: u4
      - id: class_object_id
        type: uid
      - id: stack_trace_serial_num
        type: u4
      - id: class_name_string_id
        type: uid
  tag_unload_class:
    seq:
      - id: class_serial_num
        type: u4
  tag_stack_frame:
    seq:
      - id: stack_frame_id
        type: uid
      - id: method_name_string_id
        type: uid
      - id: method_signature_string_id
        type: uid
      - id: source_file_name_string_id
        type: uid
      - id: class_serial_num
        type: u4
      - id: line_num
        type: s4
  tag_stack_trace:
    seq:
      - id: stack_trace_serial_num
        type: u4
      - id: thread_serial_num
        type: u4
      - id: num_stack_frame_ids
        type: u4
      - id: stack_frame_ids
        type: uid
        repeat: expr
        repeat-expr: num_stack_frame_ids
  tag_alloc_sites:
    seq:
      - id: bit_mask_flags
        type: u2
      - id: cutoff_ratio
        type: u4
      - id: total_live_bytes
        type: u4
      - id: total_live_instances
        type: u4
      - id: total_bytes_allocated
        type: u8
      - id: total_instances_allocated
        type: u8
      - id: num_alloc_sites
        type: u4
      - id: alloc_sites
        type: alloc_site
        repeat: expr
        repeat-expr: num_alloc_sites
    instances:
      # @todo find out default flag settings for the below
      incremental_vs_complete:
        value: bit_mask_flags & 0x01
      # @todo find out default flag settings for the below
      sorted_by_allocation_vs_line:
        value: bit_mask_flags & 0x02
      is_force_gcc_enabled:
        value: bit_mask_flags & 0x04
  alloc_site:
    seq:
      - id: array_type
        type: u1
        enum: basic_type
      - id: class_serial_num
        type: u4
      - id: stack_trace_serial_num
        type: u4
      - id: num_live_bytes
        type: u4
      - id: num_live_instances
        type: u4
      - id: num_bytes_allocated
        type: u4
      - id: num_instances_allocated
        type: u4
  tag_heap_summary:
    seq:
      - id: total_live_bytes
        type: u4
      - id: total_live_instances
        type: u4
      - id: total_bytes_allocated
        type: u8
      - id: total_instances_allocated
        type: u8
  tag_start_thread:
    seq:
      - id: thread_serial_num
        type: u4
      - id: thread_id
        type: uid
      - id: stack_frame_serial_num
        type: u4
      - id: thread_name_string_id
        type: uid
      - id: thread_group_name_string_id
        type: uid
      - id: thread_parent_group_name_string_id
        type: uid
  tag_end_thread:
    seq:
      - id: thread_serial_num
        type: u4
  tag_heap_dump:
    seq:
      - id: sub_tags
        type: sub_tag
        repeat: eos
  tag_heap_dump_segment:
    seq:
      - id: sub_tags
        type: sub_tag
        repeat: eos
  sub_tag:
    seq:
      - id: sub_record_type
        type: u1
        enum: sub_tag_type
      - id: body
        type:
          switch-on: sub_record_type
          cases:
            sub_tag_type::root_unknown: sub_tag_root_unknown
            sub_tag_type::root_jni_global: sub_tag_root_jni_global
            sub_tag_type::root_jni_local: sub_tag_root_jni_local
            sub_tag_type::root_java_frame: sub_tag_root_java_frame
            sub_tag_type::root_native_stack: sub_tag_root_native_stack
            sub_tag_type::root_sticky_class: sub_tag_root_sticky_class
            sub_tag_type::root_thread_block: sub_tag_root_thread_block
            sub_tag_type::root_monitor_used: sub_tag_root_monitor_used
            sub_tag_type::root_thread_object: sub_tag_root_thread_object
            sub_tag_type::class_dump: sub_tag_class_dump
            sub_tag_type::instance_dump: sub_tag_instance_dump
            sub_tag_type::object_array_dump: sub_tag_object_array_dump
            sub_tag_type::primitive_array_dump: sub_tag_primitive_array_dump
  sub_tag_root_unknown:
    seq:
      - id: object_id
        type: uid
  sub_tag_root_jni_global:
    seq:
      - id: object_id
        type: uid
      - id: jni_global_ref_id
        type: uid
  sub_tag_root_jni_local:
    seq:
      - id: object_id
        type: uid
      - id: thread_serial_num
        type: u4
      - id: stack_frame_num
        type: s4
  sub_tag_root_java_frame:
    seq:
      - id: object_id
        type: uid
      - id: thread_serial_num
        type: u4
      - id: stack_frame_num
        type: s4
  sub_tag_root_native_stack:
    seq:
      - id: object_id
        type: uid
      - id: thread_serial_num
        type: u4
  sub_tag_root_sticky_class:
    seq:
      - id: object_id
        type: uid
  sub_tag_root_thread_block:
    seq:
      - id: object_id
        type: uid
      - id: thread_serial_num
        type: u4
  sub_tag_root_monitor_used:
    seq:
      - id: object_id
        type: uid
  sub_tag_root_thread_object:
    seq:
      - id: object_id
        type: uid
      - id: thread_serial_num
        type: u4
      - id: stack_trace_serial_num
        type: u4
  sub_tag_class_dump:
    seq:
      - id: class_object_id
        type: uid
      - id: stack_trace_serial_num
        type: u4
      - id: super_class_object_id
        type: uid
      - id: class_loader_object_id
        type: uid
      - id: signers_object_id
        type: uid
      - id: protection_domain_object_id
        type: uid
      - id: reserved_0
        type: uid
      - id: reserved_1
        type: uid
      - id: instance_size
        type: u4
      - id: num_const_pool_entries
        type: u2
      - id: const_pool_entries
        type: const_pool_entry
        repeat: expr
        repeat-expr: num_const_pool_entries
      - id: num_static_fields
        type: u2
      - id: static_fields
        type: static_field
        repeat: expr
        repeat-expr: num_static_fields
      - id: num_instance_fields
        type: u2
      - id: instance_fields
        type: instance_field
        repeat: expr
        repeat-expr: num_instance_fields
  const_pool_entry:
    seq:
      - id: const_pool_index
        type: u2
      - id: entry_type
        type: u1
        enum: basic_type
      - id: value
        type:
          switch-on: entry_type
          cases:
            basic_type::object: uid
            basic_type::boolean: u1
            basic_type::char: u2
            basic_type::float: u4
            basic_type::double: u8
            basic_type::byte: u1
            basic_type::short: u2
            basic_type::int: u4
            basic_type::long: u8
  static_field:
    seq:
      - id: static_field_name_string_id
        type: uid
      - id: field_type
        type: u1
        enum: basic_type
      - id: value
        type:
          switch-on: field_type
          cases:
            basic_type::object: uid
            basic_type::boolean: u1
            basic_type::char: u2
            basic_type::float: u4
            basic_type::double: u8
            basic_type::byte: u1
            basic_type::short: u2
            basic_type::int: u4
            basic_type::long: u8
  instance_field:
    seq:
      - id: instance_field_name_string_id
        type: uid
      - id: field_type
        type: u1
        enum: basic_type
  sub_tag_instance_dump:
    seq:
      - id: object_id
        type: uid
      - id: stack_trace_serial_num
        type: u4
      - id: class_object_id
        type: uid
      - id: len_instance_field_values
        type: u4
      - id: instance_field_values
        size: len_instance_field_values
  sub_tag_object_array_dump:
    seq:
      - id: array_object_id
        type: uid
      - id: stack_trace_serial_num
        type: u4
      - id: num_elements
        type: u4
      - id: array_class_object_id
        type: uid
      - id: elements
        type: uid
        repeat: expr
        repeat-expr: num_elements
  sub_tag_primitive_array_dump:
    seq:
      - id: array_object_id
        type: uid
      - id: stack_trace_serial_num
        type: u4
      - id: num_elements
        type: u4
      - id: element_type
        type: u1
        enum: basic_type
      - id: elements
        type:
          switch-on: element_type
          cases:
            basic_type::object: uid
            basic_type::boolean: u1
            basic_type::char: u2
            basic_type::float: u4
            basic_type::double: u8
            basic_type::byte: u1
            basic_type::short: u2
            basic_type::int: u4
            basic_type::long: u8
        repeat: expr
        repeat-expr: num_elements
  tag_heap_dump_end:
    doc: Tag body for HEAP_DUMP_END is of size 0 bytes.
    seq:
      - id: no_value
        size: 0
  tag_cpu_samples:
    seq:
      - id: total_num_samples
        type: u4
      - id: num_cpu_traces
        type: u4
      - id: cpu_traces
        type: cpu_trace
        repeat: expr
        repeat-expr: num_cpu_traces
  cpu_trace:
    seq:
      - id: num_samples
        type: u4
      - id: stack_trace_serial_num
        type: u4
  tag_control_settings:
    seq:
      - id: bit_mask_flags
        type: u4
      - id: stack_trace_depth
        type: u2
    instances:
      is_alloc_traces_enabled:
        value: bit_mask_flags & 0x01
      is_cpu_sampling_enabled:
        value: bit_mask_flags & 0x02
