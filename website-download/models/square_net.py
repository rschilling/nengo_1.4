import hrr, nef

dimensions = 256

vocab = hrr.Vocabulary(dimensions) # create vectors

# this is gonna be provided by the vision module
# TODO: hack this into matrix and string creator

# TODO: fix the symbols of the start_matrix
start_matrix = [[1, 2, 3],
                [2, 3, 1],
                [3, 1, 2]]

symbols = ['BLANK', 'A', 'B', 'C']

# TODO: automatically create the following
rows = ['R1', 'R2', 'R3']
columns = ['C1', 'C2', 'C3']

def create_repr(matrix):
    m_repr = dict()
    # columns
    for i in range(len(start_matrix)):
        s=[] 
        for n, j in enumerate(start_matrix[i]):
            s.append(columns[n]+'*'+symbols[j])
        m_repr[rows[i]] = '+'.join(s)
    # rows
    for i in range(len(start_matrix[0])):
        s=[] 
        for n, j in enumerate([k[i] for k in start_matrix]):
            s.append(rows[n]+'*'+symbols[j])
        m_repr[columns[i]] = '+'.join(s)
    return m_repr

m_repr = create_repr(start_matrix)

# init the vocab
for k,v in m_repr.iteritems():
    vocab.parse(v)

row1 = vocab.parse('A*C1 + B*C2 + C*C3') # parse converts into vector
row2 = vocab.parse('BLANK*C1 + C*C2 + A*C3')
row3 = vocab.parse('C*C1 + BLANK*C2 + BLANK*C3')
col1 = vocab.parse('A*R1 + BLANK*R2 + C*R3')
col2 = vocab.parse('B*R1 + C*R2 + BLANK*R3')
col3 = vocab.parse('C*R1 + A*R2 + BLANK*R3')

blank_inv = vocab.parse('~BLANK') # ~ is invert
row1_inv = vocab.parse('~R1')
row2_inv = vocab.parse('~R2')
row3_inv = vocab.parse('~R3')
col1_inv = vocab.parse('~C1')
col2_inv = vocab.parse('~C2')
col3_inv = vocab.parse('~C3')
A_inv = vocab.parse('~A')
B_inv = vocab.parse('~B')
C_inv = vocab.parse('~C')

class BlankChecker(nef.SimpleNode):

    def origin_1(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        row1 = vocab.parse(m_repr['R1'])
        v1 = blank_inv * row1 # where is the blank?
        s = [vocab.keys[i] for i,v in enumerate(vocab.dot(v1)) if v>.5]
        if len(s) == 0:
            return [0]*dimensions
        v1 = vocab.parse('+'.join(s)) # clean up
        v1.normalize() # normalize
        return v1.v

    def origin_2(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        row2 = vocab.parse(m_repr['R2'])
        v2 = blank_inv * row2
        s = [vocab.keys[i] for i,v in enumerate(vocab.dot(v2)) if v>.5]
        if len(s) == 0:
            return [0]*dimensions
        v2 = vocab.parse('+'.join(s))
        v2.normalize()
        return v2.v
    
    def origin_3(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        row3 = vocab.parse(m_repr['R3'])
        v3 = blank_inv * row3
        s = [vocab.keys[i] for i,v in enumerate(vocab.dot(v3)) if v>.5]
        if len(s) == 0:
            return [0]*dimensions
        v3 = vocab.parse('+'.join(s))
        v3.normalize()
        return v3.v
    
    def origin_4(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        col1 = vocab.parse(m_repr['C1'])
        v4 = blank_inv * col1
        s = [vocab.keys[i] for i,v in enumerate(vocab.dot(v4)) if v>.5]
        if len(s) == 0:
            return [0]*dimensions
        v4 = vocab.parse('+'.join(s))
        v4.normalize()
        return v4.v
    
    def origin_5(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        col2 = vocab.parse(m_repr['C2'])
        v5 = blank_inv * col2
        s = [vocab.keys[i] for i,v in enumerate(vocab.dot(v5)) if v>.5]
        if len(s) == 0:
            return [0]*dimensions
        v5 = vocab.parse('+'.join(s))
        v5.normalize()
        return v5.v
    
    def origin_6(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        col3 = vocab.parse(m_repr['C3'])
        v6 = blank_inv * col3
        s = [vocab.keys[i] for i,v in enumerate(vocab.dot(v6)) if v>.5]
        if len(s) == 0:
            return [0]*dimensions
        v6 = vocab.parse('+'.join(s))
        v6.normalize()
        return v6.v

class SaliencyFinder(nef.SimpleNode):
    
    def termination_1(self,x,dimensions=dimensions):
        self.v1=hrr.HRR(data=x)
    
    def termination_2(self,x,dimensions=dimensions):
        self.v2=hrr.HRR(data=x)
    
    def termination_3(self,x,dimensions=dimensions):
        self.v3=hrr.HRR(data=x)
    
    def termination_4(self,x,dimensions=dimensions):
        self.v4=hrr.HRR(data=x)
    
    def termination_5(self,x,dimensions=dimensions):
        self.v5=hrr.HRR(data=x)
    
    def termination_6(self,x,dimensions=dimensions):
        self.v6=hrr.HRR(data=x)
    
    def origin_salience(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        Res = self.v1 * vocab.parse('R1') +\
              self.v2 * vocab.parse('R2') +\
              self.v3 * vocab.parse('R3') +\
              self.v4 * vocab.parse('C1') +\
              self.v5 * vocab.parse('C2') +\
              self.v6 * vocab.parse('C3')
        salience = list(vocab.dot_pairs(Res))
        s_max = max(salience)
        index = salience.index(s_max)
        pairs = vocab.key_pairs[index]
        print "Strongest salience is cell", pairs, '!'
        return vocab.parse(pairs).v

class RCStripper(nef.SimpleNode):
    
    def termination_salience(self, x, dimensions=dimensions):
        self.pairs = hrr.HRR(data=x)
    
    def origin_C(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        C_stripper = row1_inv + row2_inv + row3_inv
        C_cell = C_stripper * self.pairs # column of the cell
        return C_cell.v
    
    def origin_R(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        R_stripper = col1_inv + col2_inv + col3_inv
        R_cell = R_stripper * self.pairs # row of the cell
        return R_cell.v

# we could make a simple node to switch between inputs
# we could make 2 neural populations inhibiting each other
# we could create 2 VisionRowCol

class RCVision(nef.SimpleNode):
    
    def termination_identifier(self, x, dimensions=dimensions):
        self.x = hrr.HRR(data=x)
    
    def origin_representation(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        d=vocab.dot(self.x)
        maxd=max(d)
        maxindex=list(d).index(maxd)
        symbol=vocab.keys[maxindex]
        if symbol=='R1': return row1.v
        if symbol=='R2': return row2.v
        if symbol=='R3': return row3.v
        if symbol=='C1': return col1.v
        if symbol=='C2': return col2.v
        if symbol=='C3': return col3.v
        else: return [0]*dimensions

# we will create two estimators and take the maximum

class EstimatorC(nef.SimpleNode):
    
    def termination_vision(self, x, dimensions=dimensions):
        self.content = hrr.HRR(data=x)
    
    def origin_symbol(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        self.content = self.content*row1_inv +\
                       self.content*row2_inv +\
                       self.content*row3_inv
        guess = vocab.parse('A + B + C') - self.content
        guess_list = vocab.dot(guess)
        max_guess = max(guess_list)
        max_guess_index = list(guess_list).index(max_guess)
        final_guess = vocab.keys[max_guess_index]
        print "My guess by column is", final_guess
        return vocab.parse(final_guess).v

class EstimatorR(nef.SimpleNode):
    
    def termination_vision(self, x, dimensions=dimensions):
        self.content = hrr.HRR(data=x)
    
    def origin_symbol(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        self.content = self.content*col1_inv +\
                       self.content*col2_inv +\
                       self.content*col3_inv
        guess = vocab.parse('A + B + C') - self.content
        guess_list = vocab.dot(guess)
        max_guess = max(guess_list)
        max_guess_index = list(guess_list).index(max_guess)
        final_guess = vocab.keys[max_guess_index]
        print "My guess by row is", final_guess
        return vocab.parse(final_guess).v


class Max(nef.SimpleNode):

    def termination_C(self, x, dimensions=dimensions):
        self.content_C = hrr.HRR(data=x)

    def termination_R(self, x, dimensions=dimensions):
        self.content_R = hrr.HRR(data=x)

    def origin_symbol(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        m = max([self.content_C, self.content_R])
        return m.v


class Motor(nef.SimpleNode):

    def termination_R(self, x, dimensions=dimensions):
        self.row = hrr.HRR(data=x)

    def termination_C(self, x, dimensions=dimensions):
        self.col = hrr.HRR(data=x)

    def termination_symbol(self, x, dimensions=dimensions):
        self.sym = hrr.HRR(data=x)

    def origin_motor(self):
        if self.t_start<=0: return [0]*dimensions # small tiny little bugfix...
        row_list = list(vocab.dot(self.row))
        row_max = max(row_list)
        #if row_max > 0.5:
            #index = row_list.index(row_max)
            #r_string = vocab.keys[index]
            ##print "RRRRRRRR", r_string
            #r = rows.index(r_string)
        #else:
            #r = None
            
        #col_list = list(vocab.dot(self.col))
        #col_max = max(col_list)
        #if col_max > 0.5:
            #index = col_list.index(col_max)
            #c_string = vocab.keys[index]
            ##print "CCCCCCCC", c_string
            #c = columns.index(c_string)
        #else:
            #c = None
            
        #sym_list = list(vocab.dot(self.sym))
        #sym_max = max(sym_list)
        #if sym_max > 0.5:
            #index = sym_list.index(sym_max)
            #s_string = vocab.keys[index]
            ##print "SSSSSSSS", s_string
            #s = symbols.index(s_string)
        #else:
            #s = None

        #if r is not None and c is not None and s is not None:
            #matrix[r][c] = s
            #m_repr = create_repr(matrix)
        

################################ 
### NETWORK!

net = nef.Network('Sudoking')

blank_checker = net.add(BlankChecker('blank checker'))
saliency_finder = net.add(SaliencyFinder('saliency finder'))
rc_stripper = net.add(RCStripper('col and row stripper'))
c_vision = net.add(RCVision('col symbols'))
r_vision = net.add(RCVision('row symbols'))
c_estimator = net.add(EstimatorC('guess by col'))
r_estimator = net.add(EstimatorR('guess by row'))
max_node = net.add(Max('final guess'))
motor = net.add(Motor('motor actuator'))

net.connect(blank_checker.getOrigin('1'), saliency_finder.getTermination('1'),
            pstc = 0.01) # psct : post synaptic time constant
net.connect(blank_checker.getOrigin('2'), saliency_finder.getTermination('2'),
            pstc = 0.01)
net.connect(blank_checker.getOrigin('3'), saliency_finder.getTermination('3'),
            pstc = 0.01)
net.connect(blank_checker.getOrigin('4'), saliency_finder.getTermination('4'),
            pstc = 0.01)
net.connect(blank_checker.getOrigin('5'), saliency_finder.getTermination('5'),
            pstc = 0.01)
net.connect(blank_checker.getOrigin('6'), saliency_finder.getTermination('6'),
            pstc = 0.01)

net.connect(saliency_finder.getOrigin('salience'), rc_stripper.getTermination('salience'))

net.connect(rc_stripper.getOrigin('C'),
            c_vision.getTermination('identifier'))

net.connect(rc_stripper.getOrigin('R'),
            r_vision.getTermination('identifier'))

net.connect(c_vision.getOrigin('representation'),
            c_estimator.getTermination('vision'))

net.connect(r_vision.getOrigin('representation'),
            r_estimator.getTermination('vision'))

net.connect(r_estimator.getOrigin('symbol'),
            max_node.getTermination('R'))

net.connect(c_estimator.getOrigin('symbol'),
            max_node.getTermination('C'))

net.connect(rc_stripper.getOrigin('C'),
            motor.getTermination('C'))

net.connect(rc_stripper.getOrigin('R'),
            motor.getTermination('R'))

net.connect(max_node.getOrigin('symbol'),
            motor.getTermination('symbol'))

#net.add_to(world)

net.view()
