import esprima


class UnsupportedFeatureVisitor(esprima.NodeVisitor):
    def __init__(self):
        self.has_unsupported_feature = False
        self.found_unsupported_features = set()
        self.node_types = set()

    def visit_Object(self, obj):
        # print(obj.type)
        self.node_types.add(obj.type)
        unsupported_node_types = [
            'ArrayPattern',
            'ArrowFunctionExpression',
            'AssignmentPattern',
            'AwaitExpression',
            'ClassDeclaration',
            'ForInStatement',
            'ForOfStatement',
            'Import',
            'ObjectPattern',
            'RestElement',
            'SpreadElement',
            'Super',
            'TemplateLiteral',
            'ThrowStatement',
            'TryStatement',
            'WithStatement',
            'YieldExpression',
        ]
        if obj.type in unsupported_node_types:
            self.found_unsupported_features.add(obj.type)
            self.has_unsupported_feature = True
        if obj.type == 'BinaryExpression' and obj.operator in ['in', 'instanceof']:
            self.found_unsupported_features.add(obj.operator + ' operator')
            self.has_unsupported_feature = True
        if obj.type == 'CallExpression' and obj.callee.type == 'Identifier':
            unsupported_callee_names = [
                # 'Array',
                # 'Boolean',
                # 'Date',
                'Function',
                # 'Number',
                # 'Object',
                # 'String',
            ]
            if obj.callee.name in unsupported_callee_names:
                self.found_unsupported_features.add('call of ' + obj.callee.name)
                self.has_unsupported_feature = True
        if (obj.type in ['FunctionDeclaration', 'FunctionExpression']
                and (obj.generator or obj.isAsync)):
            self.found_unsupported_features.add(obj.type)
            self.has_unsupported_feature = True
        # property names that indicate unsupported feature
        unsupported_identifiers = [
            'arguments',
            'eval',
            'Math',
            'Promise',
            'RegExp',
            'ReferenceError',
            'Symbol',
        ]
        if obj.type == 'Identifier' and obj.name in unsupported_identifiers:
            self.found_unsupported_features.add(obj.name)
            self.has_unsupported_feature = True
        if obj.type == 'MemberExpression' and obj.property.type == 'Identifier':
            if (obj.property.name == 'keys'
                    and obj.object.type == 'Identifier'
                    and obj.object.name == 'Object'):
                self.found_unsupported_features.add(obj.property.name)
                self.has_unsupported_feature = True
            # property names that indicate unsupported feature
            unsupported_property_names = [
                'apply',
                'bind',
                'call',
                'charCodeAt',
                'constructor',
                'defineProperty',
                'fromCharCode',
                'getOwnPropertyDescriptor',
                'hasOwnProperty',
                'isPrototypeOf',
                'setPrototypeOf',
                'throws',
                'toString',
                'valueOf',
            ]
            if obj.property.name in unsupported_property_names:
                self.found_unsupported_features.add(obj.property.name)
                self.has_unsupported_feature = True
        if obj.type == 'NewExpression' and obj.callee.type == 'Identifier':
            unsupported_callee_names = [
                'Array',
                'Boolean',
                'Date',
                'Function',
                'Number',
                'Object',
                'String',
            ]
            if obj.callee.name in unsupported_callee_names:
                self.found_unsupported_features.add('new ' + obj.callee.name)
                self.has_unsupported_feature = True
        if obj.type == 'Property':
            if obj.kind in ['get', 'set']:
                self.found_unsupported_features.add(obj.kind)
                self.has_unsupported_feature = True
            if obj.method:
                self.found_unsupported_features.add('method property')
                self.has_unsupported_feature = True
            if obj.key.type == 'Identifier' and obj.key.name in ['toString', 'valueOf']:
                self.found_unsupported_features.add(obj.key.name)
                self.has_unsupported_feature = True
        if obj.type == 'UnaryExpression' and obj.operator == 'delete':
            self.found_unsupported_features.add('delete operator')
            self.has_unsupported_feature = True
        if obj.type == 'VariableDeclaration' and obj.kind in ['const', 'let']:
            self.found_unsupported_features.add(obj.kind)
            self.has_unsupported_feature = True
        return super().visit_Object(obj)